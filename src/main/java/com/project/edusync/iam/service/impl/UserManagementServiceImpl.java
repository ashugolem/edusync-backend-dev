package com.project.edusync.iam.service.impl;

import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.exception.iam.UserAlreadyExistsException;
import com.project.edusync.common.service.EmailService;
import com.project.edusync.iam.model.dto.*;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.iam.service.UserManagementService;
import com.project.edusync.uis.mapper.*;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.entity.details.LibrarianDetails;
import com.project.edusync.uis.model.entity.details.PrincipalDetails;
import com.project.edusync.uis.model.entity.details.StudentDemographics;
import com.project.edusync.uis.model.entity.details.TeacherDetails;
import com.project.edusync.uis.model.entity.medical.StudentMedicalRecord;
import com.project.edusync.uis.model.dto.profile.ComprehensiveUserProfileResponseDTO;
import com.project.edusync.uis.repository.*;
import com.project.edusync.uis.repository.details.LibrarianDetailsRepository;
import com.project.edusync.uis.repository.details.PrincipalDetailsRepository;
import com.project.edusync.uis.repository.details.StudentDemographicsRepository;
import com.project.edusync.uis.repository.details.TeacherDetailsRepository;
import com.project.edusync.uis.repository.medical.StudentMedicalRecordRepository;
import com.project.edusync.uis.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

/**
 * Implementation of UserManagementService.
 * <p>
 * This class handles the transactional creation of users across multiple tables.
 * It strictly uses the custom exception hierarchy (EdusyncException) for any failures.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementServiceImpl implements UserManagementService {

    // --- Core Identity ---
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SectionRepository sectionRepository;

    // --- UIS Core ---
    private final UserProfileRepository userProfileRepository;
    private final JsonMapper jsonMapper;

    // --- Role Entities ---
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;

    // --- Extension Repositories ---
    private final TeacherDetailsRepository teacherDetailsRepository;
    private final PrincipalDetailsRepository principalDetailsRepository;
    private final LibrarianDetailsRepository librarianDetailsRepository;
    private final StudentDemographicsRepository studentDemographicsRepository;
    private final StudentMedicalRecordRepository studentMedicalRecordRepository;

    // --- Mappers ---
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final StudentMapper studentMapper;
    private final StaffMapper staffMapper;
    private final TeacherMapper teacherMapper;
    private final PrincipalMapper principalMapper;
    private final LibrarianMapper librarianMapper;
    private final ProfileService profileService;

    // =================================================================================
    // 1. SCHOOL ADMIN
    // =================================================================================
    @Override
    @Transactional
    public User createSchoolAdmin(CreateUserRequestDTO request) {
        log.info("Process started: Creating School Admin with username: {}", request.getUsername());
        User user = createUserWithRole(request, "SCHOOL_ADMIN");
        log.info("Success: School Admin created. User ID: {}", user.getId());
        return user;
    }

    // =================================================================================
    // 2. STUDENT (Comprehensive)
    // =================================================================================
    @Override
    @Transactional
    public User createStudent(CreateStudentRequestDTO request) {
        log.info("Process started: Enrolling new Student: {}", request.getUsername());

        // 1. Create Base User & Profile (Identity Layer)
        User user = createUserWithRole(request, "STUDENT");

        // 2. Fetch Profile to link relationships
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new EdusyncException("System Error: Profile creation verification failed for user " + user.getUsername(), HttpStatus.INTERNAL_SERVER_ERROR));

        // 2. Fetch the Section
        Section section = sectionRepository.findByUuid(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section", "uuid", request.getSectionId()));
        // 3. Create Core Student Entity
        Student student = studentMapper.toStudentEntity(request);
        student.setUserProfile(profile);
        student.setSection(section);

        // Auto-generate ID if not provided (Safety net)
        if (student.getEnrollmentNumber() == null) {
            String generatedId = generateEnrollmentId();
            log.debug("Auto-generating Enrollment Number: {}", generatedId);
            student.setEnrollmentNumber(generatedId);
        }
        // Check for duplicate enrollment number
        if (studentRepository.existsByEnrollmentNumber(student.getEnrollmentNumber())) {
            throw new UserAlreadyExistsException("Student with enrollment number " + student.getEnrollmentNumber() + " already exists.");
        }

        if (student.getEnrollmentDate() == null) {
            student.setEnrollmentDate(LocalDate.now());
        }

        Student savedStudent = studentRepository.save(student);
        log.debug("Core Student record saved. ID: {}", savedStudent.getId());
        log.info("Success: Comprehensive Student enrollment complete. Enrollment #: {}", savedStudent.getEnrollmentNumber());
        return user;
    }

    // =================================================================================
    // 3. STAFF
    // =================================================================================

    @Override
    @Transactional
    public User createTeacher(CreateTeacherRequestDTO request) {
        log.info("Process started: Hiring Teacher: {}", request.getUsername());

        // 1. Create User -> Profile -> Staff
        Staff staff = createBaseStaff(request);

        // 2. Create Teacher Details Extension
        TeacherDetails details = teacherMapper.toEntity(request);
        details.setStaff(staff); // Link via @MapsId

        teacherDetailsRepository.save(details);
        log.info("Success: Teacher created with ID: {}", staff.getId());

        return staff.getUserProfile().getUser();
    }

    @Override
    @Transactional
    public User createPrincipal(CreatePrincipalRequestDTO request) {
        log.info("Process started: Appointing Principal: {}", request.getUsername());

        Staff staff = createBaseStaff(request);

        PrincipalDetails details = principalMapper.toEntity(request);
        details.setStaff(staff);

        principalDetailsRepository.save(details);
        log.info("Success: Principal created with ID: {}", staff.getId());

        return staff.getUserProfile().getUser();
    }

    @Override
    @Transactional
    public User createLibrarian(CreateLibrarianRequestDTO request) {
        log.info("Process started: Hiring Librarian: {}", request.getUsername());

        Staff staff = createBaseStaff(request);

        LibrarianDetails details = librarianMapper.toEntity(request);
        details.setStaff(staff);

        librarianDetailsRepository.save(details);
        log.info("Success: Librarian created with ID: {}", staff.getId());

        return staff.getUserProfile().getUser();
    }

    // =================================================================================
    // INTERNAL HELPERS
    // =================================================================================

    /**
     * Shared logic to create the Identity (User), Profile, and Base Staff record.
     * Prevents code duplication across different staff types.
     */
    private Staff createBaseStaff(BaseStaffRequestDTO request) {
        // A. Identity & Profile
        User user = createUserWithRole(request, request.getStaffType().name());

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new EdusyncException("System Error: Profile creation verification failed.", HttpStatus.INTERNAL_SERVER_ERROR));

        // B. Base Staff Entity
        Staff staff = staffMapper.toEntity(request);
        staff.setUserProfile(profile);
        if (!StringUtils.hasText(staff.getEmployeeId())) {
            staff.setEmployeeId(request.getUsername());
        }

        // Note: Staff ID is generated here
        return staffRepository.save(staff);
    }

    /**
     * Core logic to create the User and UserProfile.
     * Handles Validation, Password Hashing, and Role Assignment.
     */
    private User createUserWithRole(CreateUserRequestDTO request, String roleName) {
        // 1. Check for Duplicate Username
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed: Username '{}' already exists.", request.getUsername());
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken.");
        }
        // 1b. Check for Duplicate Email
        // Assuming your User repository has a method for this, or you handle it via DataIntegrityViolationException
        // We will do a manual check for cleaner error messages
         if (userRepository.existsByEmail(request.getEmail())) {
             throw new DataIntegrityViolationException("Email '" + request.getEmail() + "' already exists.");
         }

        // 2. Validate Role Existence
        Role role = roleRepository.findByName("ROLE_"+roleName)
                .orElseThrow(() -> {
                    log.error("Configuration Error: Role '{}' not found in database.", roleName);
                    return new ResourceNotFoundException("Role", "name", roleName);
                });

        // 3. Create User Entity
        User user = userMapper.toEntity(request);

        // Secure Password Generation
        String rawPassword = request.getInitialPassword() != null ?
                request.getInitialPassword() :
                request.getUsername();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        user.setRoles(Collections.singleton(role));

        User savedUser = userRepository.save(user);
        log.debug("User identity created. ID: {}", savedUser.getId());

        // 4. Create Profile Entity
        UserProfile profile = userProfileMapper.toEntity(request);
        profile.setUser(savedUser); // Foreign Key Link

        userProfileRepository.save(profile);
        log.debug("User profile created. Profile ID: {}", profile.getId());

        // 5. Send Welcome Email (Async)
        // emailService.sendWelcomeEmail(savedUser, rawPassword);

        return savedUser;
    }

    private String generateEnrollmentId() {
        return "STU-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Override
    @Transactional
    public User updateStudent(UUID studentId, UpdateStudentRequestDTO request) {
        log.info("Process started: Updating Student. UUID: {}", studentId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        UserProfile profile = student.getUserProfile();
        User user = profile.getUser();

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' already exists.");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getEnrollmentNumber())
                && !request.getEnrollmentNumber().equals(student.getEnrollmentNumber())) {
            if (studentRepository.existsByEnrollmentNumber(request.getEnrollmentNumber())) {
                throw new UserAlreadyExistsException("Student with enrollment number " + request.getEnrollmentNumber() + " already exists.");
            }
            userRepository.findByUsername(request.getEnrollmentNumber()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new UserAlreadyExistsException("Username '" + request.getEnrollmentNumber() + "' already exists.");
                }
            });
            student.setEnrollmentNumber(request.getEnrollmentNumber());
            user.setUsername(request.getEnrollmentNumber());
        }

        if (StringUtils.hasText(request.getFirstName())) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) {
            profile.setMiddleName(request.getMiddleName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            profile.setLastName(request.getLastName());
        }
        if (request.getPreferredName() != null) {
            profile.setPreferredName(request.getPreferredName());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        if (request.getRollNo() != null) {
            student.setRollNo(request.getRollNo());
        }
        if (request.getEnrollmentDate() != null) {
            student.setEnrollmentDate(request.getEnrollmentDate());
        }
        if (request.getSectionId() != null) {
            Section section = sectionRepository.findByUuid(request.getSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section", "uuid", request.getSectionId()));
            student.setSection(section);
        }

        userRepository.save(user);
        userProfileRepository.save(profile);
        studentRepository.save(student);

        log.info("Success: Student updated. UUID: {}", studentId);
        return user;
    }

    @Override
    @Transactional
    public User updateStaff(UUID staffId, UpdateStaffRequestDTO request) {
        log.info("Process started: Updating Staff. UUID: {}", staffId);

        Staff staff = staffRepository.findByUuid(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffId));

        UserProfile profile = staff.getUserProfile();
        User user = profile.getUser();

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' already exists.");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getEmployeeId()) && !request.getEmployeeId().equals(staff.getEmployeeId())) {
            if (staffRepository.existsByEmployeeId(request.getEmployeeId())) {
                throw new UserAlreadyExistsException("Staff with employee ID " + request.getEmployeeId() + " already exists.");
            }
            userRepository.findByUsername(request.getEmployeeId()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new UserAlreadyExistsException("Username '" + request.getEmployeeId() + "' already exists.");
                }
            });
            staff.setEmployeeId(request.getEmployeeId());
            user.setUsername(request.getEmployeeId());
        }

        if (StringUtils.hasText(request.getFirstName())) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) {
            profile.setMiddleName(request.getMiddleName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            profile.setLastName(request.getLastName());
        }
        if (request.getPreferredName() != null) {
            profile.setPreferredName(request.getPreferredName());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        if (StringUtils.hasText(request.getJobTitle())) {
            staff.setJobTitle(request.getJobTitle());
        }
        if (request.getHireDate() != null) {
            staff.setHireDate(request.getHireDate());
        }
        if (request.getOfficeLocation() != null) {
            staff.setOfficeLocation(request.getOfficeLocation());
        }
        if (request.getDepartment() != null) {
            staff.setDepartment(request.getDepartment());
        }
        if (request.getStaffType() != null) {
            staff.setStaffType(request.getStaffType());
        }

        userRepository.save(user);
        userProfileRepository.save(profile);
        staffRepository.save(staff);

        log.info("Success: Staff updated. UUID: {}", staffId);
        return user;
    }

    @Override
    @Transactional
    public void softDeleteStudent(UUID studentId) {
        log.info("Process started: Soft deleting Student. UUID: {}", studentId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));
        User user = student.getUserProfile().getUser();
        user.setActive(false);
        userRepository.save(user);
        log.info("Student user deactivated successfully. studentUuid={}, userId={}", studentId, user.getId());
    }

    @Override
    @Transactional
    public void softDeleteStaff(UUID staffId) {
        log.info("Process started: Soft deleting Staff. UUID: {}", staffId);

        Staff staff = staffRepository.findByUuid(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffId));
        User user = staff.getUserProfile().getUser();
        user.setActive(false);
        userRepository.save(user);
        log.info("Staff user deactivated successfully. staffUuid={}, userId={}", staffId, user.getId());
    }

    @Override
    @Transactional
    public void setStudentUserActivation(UUID studentId, boolean active) {
        log.info("Process started: Setting Student user activation. studentUuid={}, active={}", studentId, active);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        User user = student.getUserProfile().getUser();
        user.setActive(active);
        userRepository.save(user);

        log.info("Success: Student user activation updated. studentUuid={}, userId={}, active={}",
                studentId, user.getId(), active);
    }

    @Override
    @Transactional
    public void setStaffUserActivation(UUID staffId, boolean active) {
        log.info("Process started: Setting Staff user activation. staffUuid={}, active={}", staffId, active);

        Staff staff = staffRepository.findByUuid(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffId));

        User user = staff.getUserProfile().getUser();
        user.setActive(active);
        userRepository.save(user);

        log.info("Success: Staff user activation updated. staffUuid={}, userId={}, active={}",
                staffId, user.getId(), active);
    }

    @Override
    @Transactional(readOnly = true)
    public ComprehensiveUserProfileResponseDTO getStudentFullDetails(UUID studentId) {
        log.info("Process started: Fetching full Student details. UUID: {}", studentId);

        Student student = studentRepository.findByUuid(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "uuid", studentId));

        Long userId = student.getUserProfile().getUser().getId();
        ComprehensiveUserProfileResponseDTO response = profileService.getProfileByUserId(userId);

        log.info("Success: Full Student details fetched. Student UUID: {}, User ID: {}", studentId, userId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ComprehensiveUserProfileResponseDTO getStaffFullDetails(UUID staffId) {
        log.info("Process started: Fetching full Staff details. UUID: {}", staffId);

        Staff staff = staffRepository.findByUuid(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "uuid", staffId));

        Long userId = staff.getUserProfile().getUser().getId();
        ComprehensiveUserProfileResponseDTO response = profileService.getProfileByUserId(userId);

        log.info("Success: Full Staff details fetched. Staff UUID: {}, User ID: {}", staffId, userId);
        return response;
    }
}
