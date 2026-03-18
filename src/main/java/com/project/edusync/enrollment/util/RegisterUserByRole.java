package com.project.edusync.enrollment.util;

import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.entity.details.LibrarianDetails;
import com.project.edusync.uis.model.entity.details.PrincipalDetails;
import com.project.edusync.uis.model.entity.details.TeacherDetails;
import com.project.edusync.uis.model.enums.Department;
import com.project.edusync.uis.model.enums.EducationLevel;
import com.project.edusync.uis.model.enums.Gender;
import com.project.edusync.uis.model.enums.StaffType;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.repository.UserProfileRepository;
import com.project.edusync.uis.repository.details.LibrarianDetailsRepository;
import com.project.edusync.uis.repository.details.PrincipalDetailsRepository;
import com.project.edusync.uis.repository.details.TeacherDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * A helper component that encapsulates the transactional logic for creating
 * the full entity graph for a user (User, UserProfile, and Student/Staff details).
 * This class is called by BulkImportServiceImpl *within* an existing transaction.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RegisterUserByRole {

    // --- Core Repositories ---
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    // --- Staff Details Repositories ---
    private final TeacherDetailsRepository teacherDetailsRepository;
    private final LibrarianDetailsRepository librarianDetailsRepository;
    private final PrincipalDetailsRepository principalDetailsRepository;
    // (Add other details repositories here)

    // --- Utilities ---
    private final PasswordEncoder passwordEncoder;
    private final CsvValidationHelper validationHelper;


    /**
     * Creates and saves the core User entity.
     * This is the foundational block for both Students and Staff.
     *
     * @param email The user's unique email.
     * @param username The user's unique username (e.g., enrollment number or employeeId).
     * @param DEFAULT_PASSWORD The plain-text default password to be hashed.
     * @param role The pre-fetched Role entity to assign.
     * @return The newly persisted User entity.
     */
    public User RegisterUser(String email,
                             String username,
                             String DEFAULT_PASSWORD,
                             Role role){
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setActive(true);
        user.setRoles(Collections.singleton(role));

        User savedUser = userRepository.save(user);
        log.info("Successfully created user: {}", user.getUsername());
        return savedUser;
    }

    /**
     * Creates and saves the core UserProfile entity.
     *
     * @param firstName The user's first name.
     * @param lastName The user's last name.
     * @param middleName The user's middle name (can be null/empty).
     * @param dob The user's date of birth.
     * @param gender The user's gender.
     * @param user The parent User entity to link against.
     * @return The newly persisted UserProfile entity.
     */
    public UserProfile RegisterUserProfile(String firstName,
                                           String lastName,
                                           String middleName,
                                           LocalDate dob,
                                           Gender gender,
                                           User user){
        UserProfile userProfile = new UserProfile();
        userProfile.setFirstName(firstName);
        userProfile.setLastName(lastName);
        userProfile.setMiddleName(middleName);
        userProfile.setDateOfBirth(dob);
        userProfile.setGender(gender);
        userProfile.setUser(user);

        UserProfile savedUserProfile = userProfileRepository.save(userProfile);
        log.info("Successfully created User Profile for : {}", user.getUsername());
        return savedUserProfile;
    }

    /**
     * Orchestrates the full creation of a Student entity graph
     * (User -> UserProfile -> Student).
     * Runs within the service-layer transaction.
     *
     * @param email The student's email.
     * @param enrollmentNumber The student's unique enrollment number (used as username).
     * @param DEFAULT_PASSWORD The default password.
     * @param studentRole The pre-fetched "ROLE_STUDENT".
     * @param firstName First name.
     * @param lastName Last name.
     * @param middleName Middle name.
     * @param dob Date of birth.
     * @param gender Gender.
     * @param enrollmentDate Date of enrollment.
     * @param section The pre-fetched Section entity.
     */
    @Transactional
    public void RegisterStudent(
            String email,
            String enrollmentNumber,
            String DEFAULT_PASSWORD,
            Role studentRole,
            String firstName,
            String lastName,
            String middleName,
            LocalDate dob,
            Gender gender,
            LocalDate enrollmentDate,
            Section section,
            Integer rollNo
    ){
        log.info("Attempting to create User : {}", enrollmentNumber);
        User user = RegisterUser(email, enrollmentNumber, DEFAULT_PASSWORD, studentRole);

        log.info("Attempting to create User Profile : {}", enrollmentNumber);
        UserProfile userProfile = RegisterUserProfile(firstName, lastName, middleName, dob, gender, user);

        // Create the final Student entity
        log.info("Attempting to create Student: {} with username : {}", firstName, enrollmentNumber);
        Student student = new Student();
        student.setEnrollmentNumber(enrollmentNumber);
        student.setEnrollmentDate(enrollmentDate);
        student.setRollNo(rollNo);
        student.setActive(true);
        student.setUserProfile(userProfile);
        student.setSection(section);

        studentRepository.save(student);
        log.info("Successfully created student: {}", email);
    }

    /**
     * Orchestrates the full creation of a Staff entity graph
     * (User -> UserProfile -> Staff -> StaffDetails).
     * Runs within the service-layer transaction.
     *
     * @param row The full CSV row, used to parse role-specific details.
     * @throws RuntimeException if any parsing or validation fails.
     */
    public void RegisterStaff(
            String email,
            String employeeId,
            String DEFAULT_PASSWORD,
            Role staffRole,
            String firstName,
            String lastName,
            String middleName,
            LocalDate dob,
            Gender gender,
            LocalDate joiningDate,
            String jobTitle,
            Department department,
            StaffType staffType,
            String[] row
    ) { // <-- No 'throws Exception' needed, RuntimeExceptions will bubble up

        log.info("Attempting to create User for staff: {}", email);
        User user = RegisterUser(email, employeeId, DEFAULT_PASSWORD, staffRole); // Staff employeeId is their email

        log.info("Attempting to create User Profile for staff: {}", email);
        UserProfile userProfile = RegisterUserProfile(firstName, lastName, middleName, dob, gender, user);

        // Create and save the base Staff object
        Staff staff = new Staff();
        staff.setEmployeeId(employeeId);
        staff.setHireDate(joiningDate);
        staff.setJobTitle(jobTitle);
        staff.setDepartment(department);
        staff.setStaffType(staffType);
        staff.setActive(true);
        staff.setUserProfile(userProfile);

        log.info("Attempting to create base Staff record for: {}", email);
        Staff savedStaff = staffRepository.save(staff);
        log.info("Successfully created base Staff with ID: {}", savedStaff.getId());

        // --- Create Staff Details based on StaffType ---
        // Any exception thrown here (e.g., from validationHelper)
        // will bubble up, fail the transaction, and be caught by the service.
        switch (staffType) {
            case TEACHER:
                registerTeacherDetails(savedStaff, row);
                break;
            case LIBRARIAN:
                registerLibrarianDetails(savedStaff, row);
                break;
            case PRINCIPAL:
                registerPrincipalDetails(savedStaff, row);
                break;
            // TODO: Add cases for SECURITY_GUARD, ADMIN_STAFF, etc.
            default:
                log.warn("No specific details table configured for StaffType: {}", staffType);
        }

        log.info("Successfully created staff and details for: {}", email);
    }

    /**
     * Parses Teacher-specific columns from the row and saves the TeacherDetails.
     * Assumes CSV columns 11, 12, 13, 14.
     */
    private void registerTeacherDetails(Staff staff, String[] row) {
        log.info("Registering TeacherDetails for staff ID: {}", staff.getId());
        TeacherDetails details = new TeacherDetails();
        details.setStaff(staff);

        // This will now use the List<String> fields from the corrected entity
        details.setCertifications(List.of(validationHelper.validateString(row[11], "certifications").split(",")));
        details.setSpecializations(List.of(validationHelper.validateString(row[12], "specializations").split(",")));
        details.setYearsOfExperience(Integer.parseInt(validationHelper.validateString(row[13], "yearsOfExperience")));
        details.setEducationLevel(validationHelper.parseEnum(EducationLevel.class, row[14], "educationLevel"));

        teacherDetailsRepository.save(details);
    }

    /**
     * Parses Librarian-specific columns from the row and saves the LibrarianDetails.
     * Assumes CSV column 11.
     */
    private void registerLibrarianDetails(Staff staff, String[] row) {
        log.info("Registering LibrarianDetails for staff ID: {}", staff.getId());
        LibrarianDetails details = new LibrarianDetails();
        details.setStaff(staff);
        String mlisValue = row[19];

        if (mlisValue == null || mlisValue.isBlank()) {
            details.setMlisDegree(false); // Default to false if blank
        } else {
            // Only parse if not blank. Boolean.parseBoolean is safe.
            details.setMlisDegree(Boolean.parseBoolean(mlisValue));
        }
        String permissionsValue = row[18];
        if (permissionsValue != null && !permissionsValue.isBlank()) {
            details.setLibrarySystemPermissions(List.of(permissionsValue.split(",")));
        } else {
            details.setLibrarySystemPermissions(Collections.emptyList()); // Set as empty list
        }
        librarianDetailsRepository.save(details);
    }

    /**
     * Parses Principal-specific columns from the row and saves the PrincipalDetails.
     */
    private void registerPrincipalDetails(Staff staff, String[] row) {
        log.info("Registering PrincipalDetails for staff ID: {}", staff.getId());
        PrincipalDetails details = new PrincipalDetails();
        details.setStaff(staff);

        // TODO: Add parsing logic for principal details from 'row' as needed
        // e.g., details.setBudgetApprovalLimit(new BigDecimal(validationHelper.validateString(row[11], "budgetLimit")));

        principalDetailsRepository.save(details);
    }
}