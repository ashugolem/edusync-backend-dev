package com.project.edusync.enrollment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.common.exception.enrollment.InvalidCsvHeaderException;
import com.project.edusync.common.exception.enrollment.RelatedResourceNotFoundException;
import com.project.edusync.common.exception.enrollment.ResourceDuplicateException;
import com.project.edusync.enrollment.model.dto.BulkImportProgressEvent;
import com.project.edusync.enrollment.model.dto.BulkImportReportDTO;
import com.project.edusync.enrollment.service.BulkImportService;
import com.project.edusync.enrollment.service.SseEmitterRegistry;
import com.project.edusync.enrollment.util.CsvValidationHelper;
import com.project.edusync.enrollment.util.RegisterUserByRole;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.uis.model.enums.*;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for bulk user import via CSV.
 * This implementation is **resilient** (processing one row at a time) and
 * **optimized** (pre-caches static data like Roles and Sections).
 *
 * It orchestrates the import by:
 * 1. Validating the CSV header structure based on userType.
 * 2. Pre-fetching and caching static data (Roles, Sections) for performance.
 * 3. Looping through the CSV file one row at a time.
 * 4. Calling a separate, transactional method for each row.
 * 5. Wrapping each row's processing in a try-catch block to ensure that
 * one bad row does not stop the entire import.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BulkImportServiceImpl implements BulkImportService {

    // --- Constants ---
    private static final String USER_TYPE_STUDENTS = "students";
    private static final String USER_TYPE_STAFF = "staff";
    private static final String ROLE_STUDENT = "ROLE_STUDENT";

    // --- CSV Header Definitions (NEW) ---
    // (This enforces strict column order for the import)
    private static final List<String> STUDENT_HEADER = Arrays.asList(
            "firstName", "lastName", "middleName", "email", "dateOfBirth",
            "rollNo", "gender", "enrollmentNumber", "enrollmentDate",
            "className", "sectionName"
    );

    // Common staff fields + all *possible* specific fields
    // This provides a single, verifiable header for the "staff.csv"
    private static final List<String> STAFF_HEADER = Arrays.asList(
            // Common Staff (0-10)
            "firstName", "lastName", "middleName", "email", "dateOfBirth",
            "gender", "employeeId", "joiningDate", "jobTitle", "department", "staffType",
            // Teacher (11-15)
            "certifications", "specializations", "yearsOfExperience", "educationLevel", "stateLicenseNumber",
            // Principal (16-17)
            "administrativeCertifications", "schoolLevelManaged",
            // Librarian (18-19)
            "librarySystemPermissions", "mlisDegree",
            // Security (20-21)
            "assignedGate", "shiftTiming"
    );


    @Value("${edusync.bulk-import.default-password:Welcome@123}")
    private String DEFAULT_PASSWORD;

    // --- Repositories & Services (all final) ---
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final SectionRepository sectionRepository;
    private final CsvValidationHelper validationHelper;
    private final RegisterUserByRole registerUserByRole;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Emits a progress event to the SSE emitter for the given session.
     * Silently swallows IO errors so that an SSE glitch never aborts the import.
     */
    private void emitEvent(String sessionId, BulkImportProgressEvent event) {
        if (sessionId == null) return;
        SseEmitter emitter = sseEmitterRegistry.get(sessionId);
        if (emitter == null) return;
        try {
            emitter.send(
                SseEmitter.event()
                    .name(event.getEventType())
                    .data(objectMapper.writeValueAsString(event))
            );
        } catch (IOException e) {
            log.warn("Failed to emit SSE event for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Orchestrates the import process.
     * This method is NOT transactional itself.
     */
    @Override
    public BulkImportReportDTO importUsers(MultipartFile file, String userType, String sessionId) throws IOException {

        log.info("Building caches for roles and sections...");
        final Map<String, Role> roleCache = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getName, role -> role));

        final Map<String, Section> sectionCache = sectionRepository.findAllWithClass().stream()
                .collect(Collectors.toMap(
                        s -> s.getAcademicClass().getName() + ":" + s.getSectionName(),
                        s -> s
                ));
        log.info("Caches built with {} roles and {} sections. Starting row processing...", roleCache.size(), sectionCache.size());

        BulkImportReportDTO report = new BulkImportReportDTO();
        report.setStatus("PROCESSING");
        int rowNumber = 1, successCount = 0, failureCount = 0;

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            // --- HEADER VALIDATION ---
            String[] header = csvReader.readNext();
            if (header == null) {
                // USE: InvalidCsvHeaderException (Fatal)
                throw new InvalidCsvHeaderException("File is empty or header is missing.");
            }

            final List<String> expectedHeader;
            if (USER_TYPE_STUDENTS.equalsIgnoreCase(userType)) {
                expectedHeader = STUDENT_HEADER;
            } else if (USER_TYPE_STAFF.equalsIgnoreCase(userType)) {
                expectedHeader = STAFF_HEADER;
            } else {
                throw new IllegalArgumentException("Invalid userType: " + userType);
            }

            List<String> actualHeader = Arrays.asList(header);
            if (!actualHeader.equals(expectedHeader)) {
                log.warn("CSV Header Validation FAILED. Expected: {}, Found: {}", expectedHeader, actualHeader);
                // USE: InvalidCsvHeaderException (Fatal)
                throw new InvalidCsvHeaderException(
                        String.format("Invalid CSV header. Expected: %s, Found: %s", expectedHeader, actualHeader)
                );
            }
            log.info("CSV Header validation passed.");
            // --- End Header Validation ---


            String[] row;
            while ((row = csvReader.readNext()) != null) {
                rowNumber++;
                // Extract a human-readable identifier for SSE events (email is col 3)
                String identifier = (row.length > 3 && row[3] != null && !row[3].isBlank())
                        ? row[3] : "row-" + rowNumber;
                try {
                    if (USER_TYPE_STUDENTS.equalsIgnoreCase(userType)) {
                        processStudentRow(row, roleCache, sectionCache);
                    } else if (USER_TYPE_STAFF.equalsIgnoreCase(userType)) {
                        routeStaffRowProcessing(row, roleCache);
                    }
                    successCount++;

                    // ── Emit ROW_SUCCESS ──────────────────────────────────────────
                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_SUCCESS")
                            .identifier(identifier)
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());

                } catch (Exception e) {
                    failureCount++;
                    String errorMessage = e.getMessage();
                    String error = String.format("Row %d: %s", rowNumber, errorMessage);
                    report.getErrorMessages().add(error);
                    log.warn("Failed to process row {}: {}", rowNumber, errorMessage);

                    // ── Emit ROW_FAILURE ──────────────────────────────────────────
                    emitEvent(sessionId, BulkImportProgressEvent.builder()
                            .rowNumber(rowNumber - 1)
                            .eventType("ROW_FAILURE")
                            .identifier(identifier)
                            .errorMessage(errorMessage)
                            .successCount(successCount)
                            .failureCount(failureCount)
                            .build());
                }
            }
        } catch (CsvValidationException | InvalidCsvHeaderException e) {
            report.setStatus("FAILED");
            report.getErrorMessages().add("Fatal Error: " + e.getMessage());

            // ── Emit JOB_FAILED ───────────────────────────────────────────────────
            emitEvent(sessionId, BulkImportProgressEvent.builder()
                    .eventType("JOB_FAILED")
                    .errorMessage("Fatal Error: " + e.getMessage())
                    .successCount(0)
                    .failureCount(0)
                    .build());
            sseEmitterRegistry.complete(sessionId);
            return report;
        }

        report.setStatus("COMPLETED");
        report.setTotalRows(rowNumber - 1);
        report.setSuccessCount(successCount);
        report.setFailureCount(failureCount);

        // ── Emit JOB_COMPLETE ─────────────────────────────────────────────────────
        emitEvent(sessionId, BulkImportProgressEvent.builder()
                .eventType("JOB_COMPLETE")
                .totalRows(rowNumber - 1)
                .successCount(successCount)
                .failureCount(failureCount)
                .build());
        sseEmitterRegistry.complete(sessionId);
        return report;
    }

    /**
     * Processes and validates a single student row.
     * This method is marked @Transactional.
     */
    @Transactional(rollbackFor = Exception.class)
    public void processStudentRow(String[] row, Map<String, Role> roleCache, Map<String, Section> sectionCache) throws Exception {
        // 1. --- Parse & Validate Data (per students.csv spec) ---
        // This section will now throw DataParsingException if it fails
        String firstName = validationHelper.validateString(row[0], "firstName");
        String lastName = validationHelper.validateString(row[1], "lastName");
        String middleName = row[2];
        String email = validationHelper.validateEmail(row[3]);
        LocalDate dob = validationHelper.parseDate(row[4], "dateOfBirth");
        Integer rollNo = validationHelper.parseInt(row[5], "rollNo");
        Gender gender = validationHelper.parseEnum(Gender.class, row[6], "gender");
        String enrollmentNumber = validationHelper.validateString(row[7], "enrollmentNumber");
        LocalDate enrollmentDate = validationHelper.parseDate(row[8], "enrollmentDate");
        String className = validationHelper.validateString(row[9], "className");
        String sectionName = validationHelper.validateString(row[10], "sectionName");

        // 2. --- Validate Business Logic & Foreign Keys ---
        if (userRepository.existsByUsername(enrollmentNumber)) {
            // USE: ResourceDuplicateException
            throw new ResourceDuplicateException("User with username '" + enrollmentNumber + "' already exists.");
        }
        if (userRepository.existsByEmail(email)) {
            // USE: ResourceDuplicateException
            throw new ResourceDuplicateException("User with email '" + email + "' already exists.");
        }
        if (studentRepository.existsByEnrollmentNumber(enrollmentNumber)) {
            // USE: ResourceDuplicateException
            throw new ResourceDuplicateException("Student with enrollment number '" + enrollmentNumber + "' already exists.");
        }

        Section section = sectionCache.get(className + ":" + sectionName);
        if (section == null) {
            // USE: RelatedResourceNotFoundException
            throw new RelatedResourceNotFoundException("Section not found for class '" + className + "' and section '" + sectionName + "'.");
        }

        Role studentRole = roleCache.get(ROLE_STUDENT);
        if (studentRole == null) {
            // USE: RelatedResourceNotFoundException
            throw new RelatedResourceNotFoundException("CRITICAL: " + ROLE_STUDENT + " not found in database.");
        }

        // 3. --- Delegate creation to the helper ---
        registerUserByRole.RegisterStudent(
                email, enrollmentNumber, DEFAULT_PASSWORD, studentRole,
                firstName, lastName, middleName, dob, gender,
                enrollmentDate, section, rollNo
        );
    }


    /**
     * (NEW) Transactional router for staff processing.
     * This method is the single transactional entry point for a staff row.
     * It parses *only* the staffType to determine which specific
     * processing method to call.
     *
     * @param row The raw String[] from the CSV.
     * @param roleCache The pre-fetched Role map.
     * @throws Exception if any validation or database constraint fails.
     */
    @Transactional(rollbackFor = Exception.class)
    public void routeStaffRowProcessing(String[] row, Map<String, Role> roleCache) throws Exception {
        // This will throw DataParsingException if row[10] is invalid
        StaffType staffType = validationHelper.parseEnum(StaffType.class, row[10], "staffType");

        switch (staffType) {
            case TEACHER:
                processTeacherRow(row, roleCache);
                break;
            case PRINCIPAL:
                processPrincipalRow(row, roleCache);
                break;
            case LIBRARIAN:
                processLibrarianRow(row, roleCache);
                break;
            default:
                throw new IllegalArgumentException("Unsupported staff type '" + staffType + "' for bulk import.");
        }
    }

    /**
     * (NEW) Private helper to process and save a single Teacher row.
     * This method is NOT transactional; it runs inside the transaction
     * of `routeStaffRowProcessing`.
     */
    private void processTeacherRow(String[] row, Map<String, Role> roleCache) throws Exception {
        // 1. --- Parse Common Staff Fields (Indices 0-9) ---
        String firstName = validationHelper.validateString(row[0], "firstName");
        String lastName = validationHelper.validateString(row[1], "lastName");
        String middleName = row[2]; // Optional
        String email = validationHelper.validateEmail(row[3]);
        LocalDate dob = validationHelper.parseDate(row[4], "dateOfBirth");
        Gender gender = validationHelper.parseEnum(Gender.class, row[5], "gender");
        String employeeId = validationHelper.validateString(row[6], "employeeId");
        LocalDate joiningDate = validationHelper.parseDate(row[7], "joiningDate");
        String jobTitle = validationHelper.validateString(row[8], "jobTitle");
        Department department = validationHelper.parseEnum(Department.class, row[9], "department");

        // 2. --- Parse Teacher-Specific Fields (Indices 11-15) ---
        String certifications = row[11]; // Assuming JSON string or CSV
        String specializations = row[12]; // Assuming JSON string or CSV
        Integer yearsOfExperience = validationHelper.parseInt(row[13], "yearsOfExperience");
        EducationLevel educationLevel = validationHelper.parseEnum(
                EducationLevel.class, row[14], "educationLevel"
        );
        String stateLicenseNumber = row[15];

        // 3. --- Validate Business Logic ---
        if (userRepository.existsByEmail(email)) {
            throw new ResourceDuplicateException("User with email '" + email + "' already exists.");
        }
        if (staffRepository.existsByEmployeeId(employeeId)) {
            throw new ResourceDuplicateException("Staff with employee ID '" + employeeId + "' already exists.");
        }

        Role staffRole = roleCache.get("ROLE_TEACHER");
        if (staffRole == null) {
            throw new RelatedResourceNotFoundException("CRITICAL: Role 'ROLE_TEACHER' not found in database.");
        }

        // 4. --- Delegate creation to the (refactored) helper ---
        registerUserByRole.RegisterStaff(email, employeeId, DEFAULT_PASSWORD, staffRole,
                firstName, lastName, middleName, dob, gender, joiningDate, jobTitle,
                department, StaffType.TEACHER, row);
    }

    /**
     * (NEW) Private helper to process and save a single Principal row.
     */
    private void processPrincipalRow(String[] row, Map<String, Role> roleCache) throws Exception {
        // 1. --- Parse Common Staff Fields (Indices 0-9) ---
        String firstName = validationHelper.validateString(row[0], "firstName");
        String lastName = validationHelper.validateString(row[1], "lastName");
        String middleName = row[2]; // Optional
        String email = validationHelper.validateEmail(row[3]);
        LocalDate dob = validationHelper.parseDate(row[4], "dateOfBirth");
        Gender gender = validationHelper.parseEnum(Gender.class, row[5], "gender");
        String employeeId = validationHelper.validateString(row[6], "employeeId");
        LocalDate joiningDate = validationHelper.parseDate(row[7], "joiningDate");
        String jobTitle = validationHelper.validateString(row[8], "jobTitle");
        Department department = validationHelper.parseEnum(Department.class, row[9], "department");

        // 2. --- Parse Principal-Specific Fields (Indices 16-17) ---
        String adminCertifications = row[16]; // Assuming JSON string or CSV
        SchoolLevel schoolLevel = validationHelper.parseEnum(
                SchoolLevel.class, row[17], "schoolLevelManaged"
        );

        // 3. --- Validate Business Logic ---
        if (userRepository.existsByEmail(email)) {
            throw new ResourceDuplicateException("User with email '" + email + "' already exists.");
        }
        if (staffRepository.existsByEmployeeId(employeeId)) {
            throw new ResourceDuplicateException("Staff with employee ID '" + employeeId + "' already exists.");
        }

        Role staffRole = roleCache.get("ROLE_PRINCIPAL");
        if (staffRole == null) {
            throw new RelatedResourceNotFoundException("CRITICAL: Role 'ROLE_PRINCIPAL' not found in database.");
        }

        // 4. --- Delegate creation to the (refactored) helper ---
        registerUserByRole.RegisterStaff(email, employeeId, DEFAULT_PASSWORD, staffRole,
                firstName, lastName, middleName, dob, gender, joiningDate, jobTitle,
                department, StaffType.PRINCIPAL, row);
    }

    /**
     * (NEW) Private helper to process and save a single Librarian row.
     */
    private void processLibrarianRow(String[] row, Map<String, Role> roleCache) throws Exception {
        // 1. --- Parse Common Staff Fields (Indices 0-9) ---
        String firstName = validationHelper.validateString(row[0], "firstName");
        String lastName = validationHelper.validateString(row[1], "lastName");
        String middleName = row[2]; // Optional
        String email = validationHelper.validateEmail(row[3]);
        LocalDate dob = validationHelper.parseDate(row[4], "dateOfBirth");
        Gender gender = validationHelper.parseEnum(Gender.class, row[5], "gender");
        String employeeId = validationHelper.validateString(row[6], "employeeId");
        LocalDate joiningDate = validationHelper.parseDate(row[7], "joiningDate");
        String jobTitle = validationHelper.validateString(row[8], "jobTitle");
        Department department = validationHelper.parseEnum(Department.class, row[9], "department");

        // 3. --- Validate Business Logic ---
        if (userRepository.existsByEmail(email)) {
            throw new ResourceDuplicateException("User with email '" + email + "' already exists.");
        }
        if (staffRepository.existsByEmployeeId(employeeId)) {
            throw new ResourceDuplicateException("Staff with employee ID '" + employeeId + "' already exists.");
        }

        Role staffRole = roleCache.get("ROLE_LIBRARIAN");
        if (staffRole == null) {
            throw new RelatedResourceNotFoundException("CRITICAL: Role 'ROLE_LIBRARIAN' not found in database.");
        }

        // 4. --- Delegate creation to the (refactored) helper ---
        registerUserByRole.RegisterStaff(email, employeeId, DEFAULT_PASSWORD, staffRole,
                firstName, lastName, middleName, dob, gender, joiningDate, jobTitle,
                department, StaffType.LIBRARIAN, row);
    }
}