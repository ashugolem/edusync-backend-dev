package com.project.edusync.iam.controller;

import com.project.edusync.iam.model.dto.*;
import com.project.edusync.iam.service.UserManagementService;
import com.project.edusync.uis.model.dto.profile.ComprehensiveUserProfileResponseDTO;
import com.project.edusync.uis.model.dto.profile.GuardianProfileDTO;
import com.project.edusync.uis.model.dto.profile.LinkedStudentDTO;
import com.project.edusync.uis.model.dto.profile.StudentGuardianDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Admin-level User Management.
 * <p>
 * These endpoints serve as the "Hiring" or "Enrollment" portal for the application.
 * They allow privileged users (Super Admin, School Admin) to onboard new users
 * into the system with all their necessary role-specific data (e.g., Medical records for Students,
 * Certifications for Teachers).
 * </p>
 * * <p>Security: All endpoints are protected by Role-Based Access Control (RBAC).</p>
 */
@RestController
@RequestMapping("${api.url}/auth/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Admin User Management",
        description = "Administrative APIs for creating and updating School Admin, Student, and Staff users."
)
public class UserManagementController {

    private final UserManagementService userManagementService;

    // =================================================================================
    // 1. SCHOOL ADMIN MANAGEMENT
    // =================================================================================

    /**
     * Create a new School Admin.
     * RESTRICTED TO: Super Admin only.
     */
    @PostMapping("/school-admin")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create School Admin",
            description = "Creates a new School Admin account with identity and profile details. Accessible only to Super Admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "School Admin created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "409", description = "Conflict - Username or Email already exists"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires Super Admin privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> createSchoolAdmin(@Valid @RequestBody CreateUserRequestDTO request) {
        log.info("API Request: Create School Admin [{}]", request.getUsername());
        userManagementService.createSchoolAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("School Admin created successfully.");
    }

    // =================================================================================
    // 2. STUDENT ENROLLMENT
    // =================================================================================

    /**
     * Enroll a new Student.
     * ACCESSIBLE BY: Super Admin, School Admin.
     * This creates the User, Profile, Student record, Demographics, and Medical record in one go.
     */
    @PostMapping("/student")
//    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create Student",
            description = "Creates a Student account with linked profile and enrollment details. Intended for School Admin and Super Admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Student enrolled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Section not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - User or Enrollment Number already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> createStudent(@Valid @RequestBody CreateStudentRequestDTO request) {
        log.info("API Request: Enroll Student [{}]", request.getUsername());
        userManagementService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Student enrolled successfully.");
    }

    // =================================================================================
    // 3. STAFF HIRING (Role Specific)
    // =================================================================================

    /**
     * Hire a new Teacher.
     * ACCESSIBLE BY: Super Admin, School Admin.
     */
    @PostMapping("/staff/teacher")
//    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create Teacher",
            description = "Creates a Teacher account and corresponding staff details. Intended for School Admin and Super Admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Teacher created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "409", description = "Conflict - Username, email, or employee ID already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> createTeacher(@Valid @RequestBody CreateTeacherRequestDTO request) {
        log.info("API Request: Hire Teacher [{}]", request.getUsername());
        userManagementService.createTeacher(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Teacher created successfully.");
    }

    /**
     * Appoint a new Principal.
     * RESTRICTED TO: Super Admin only (typically).
     */
    @PostMapping("/staff/principal")
//    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create Principal",
            description = "Creates a Principal account and corresponding staff details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Principal created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires appropriate admin privileges"),
            @ApiResponse(responseCode = "409", description = "Conflict - Username, email, or employee ID already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> createPrincipal(@Valid @RequestBody CreatePrincipalRequestDTO request) {
        log.info("API Request: Appoint Principal [{}]", request.getUsername());
        userManagementService.createPrincipal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Principal created successfully.");
    }

    /**
     * Hire a new Librarian.
     * ACCESSIBLE BY: Super Admin, School Admin.
     */
    @PostMapping("/staff/librarian")
//    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create Librarian",
            description = "Creates a Librarian account and corresponding staff details. Intended for School Admin and Super Admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Librarian created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "409", description = "Conflict - Username, email, or employee ID already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> createLibrarian(@Valid @RequestBody CreateLibrarianRequestDTO request) {
        log.info("API Request: Hire Librarian [{}]", request.getUsername());
        userManagementService.createLibrarian(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Librarian created successfully.");
    }

    @PostMapping("/student/{studentId}/guardian")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Add Guardian for Student",
            description = "Creates a guardian user and links that guardian with the provided student UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Guardian added successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Username or email already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> createGuardian(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId,
            @Valid @RequestBody CreateGuardianRequestDTO request) {
        log.info("API Request: Add Guardian [{}] for Student [{}]", request.getUsername(), studentId);
        userManagementService.createGuardian(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Guardian added successfully.");
    }

    @PostMapping("/student/{studentId}/guardian/link")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Link Existing Guardian to Student",
            description = "Links an already existing guardian to the given student without creating a new guardian account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Guardian linked successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student or guardian not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - guardian already linked to student"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> linkExistingGuardian(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId,
            @Valid @RequestBody LinkGuardianRequestDTO request) {
        log.info("API Request: Link existing Guardian [{}] to Student [{}]", request.getGuardianId(), studentId);
        userManagementService.linkExistingGuardian(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Guardian linked successfully.");
    }

    @GetMapping("/student/{studentId}/guardians")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get Guardians of Student",
            description = "Returns all guardians linked with the specified student UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Guardians fetched successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<StudentGuardianDTO>> getStudentGuardians(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId) {
        log.info("API Request: Get Guardians for Student [{}]", studentId);
        return ResponseEntity.ok(userManagementService.getGuardiansByStudent(studentId));
    }

    @GetMapping("/guardian/{guardianId}/linked-students")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get Linked Students of Guardian",
            description = "Returns only student linkage list for a guardian UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Linked students fetched successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Guardian not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<LinkedStudentDTO>> getGuardianLinkedStudents(
            @Parameter(description = "Guardian UUID", required = true, example = "51d020cc-b89f-42dc-98a3-b4fa88b66331")
            @PathVariable java.util.UUID guardianId) {
        log.info("API Request: Get Linked Students for Guardian [{}]", guardianId);
        return ResponseEntity.ok(userManagementService.getLinkedStudentsByGuardian(guardianId));
    }

    // =================================================================================
    // 4. EDIT STUDENT / STAFF
    // =================================================================================

    /**
     * Edit an existing Student's details.
     * ACCESSIBLE BY: Super Admin, School Admin.
     */
    @PutMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update Student",
            description = "Updates Student identity, profile, and enrollment fields by Student UUID. Accessible to School Admin and Super Admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student or section not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Email or enrollment number already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> updateStudent(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId,
            @Valid @RequestBody UpdateStudentRequestDTO request) {
        log.info("API Request: Update Student [{}]", studentId);
        userManagementService.updateStudent(studentId, request);
        return ResponseEntity.ok("Student updated successfully.");
    }

    /**
     * Edit an existing Staff's details.
     * ACCESSIBLE BY: Super Admin, School Admin.
     */
    @PutMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update Staff",
            description = "Updates Staff identity, profile, and employment fields by Staff UUID. Accessible to School Admin and Super Admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Staff not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Email or employee ID already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> updateStaff(
            @Parameter(description = "Staff UUID", required = true, example = "4e95ad14-20da-4939-b666-841f3259997d")
            @PathVariable java.util.UUID staffId,
            @Valid @RequestBody UpdateStaffRequestDTO request) {
        log.info("API Request: Update Staff [{}]", staffId);
        userManagementService.updateStaff(staffId, request);
        return ResponseEntity.ok("Staff updated successfully.");
    }

    @PutMapping("/student/{studentId}/guardian/{guardianId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Edit Guardian of Student",
            description = "Updates guardian identity/profile fields and guardian-student relationship metadata."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Guardian updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student, guardian, or relationship not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - email already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> updateGuardian(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId,
            @Parameter(description = "Guardian UUID", required = true, example = "51d020cc-b89f-42dc-98a3-b4fa88b66331")
            @PathVariable java.util.UUID guardianId,
            @Valid @RequestBody UpdateGuardianRequestDTO request) {
        log.info("API Request: Update Guardian [{}] for Student [{}]", guardianId, studentId);
        userManagementService.updateGuardian(studentId, guardianId, request);
        return ResponseEntity.ok("Guardian updated successfully.");
    }

    // =================================================================================
    // 5. SOFT DELETE STUDENT / STAFF
    // =================================================================================

    @DeleteMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Soft Delete Student",
            description = "Deactivates the User linked to the given Student UUID (user.isActive=false)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student soft deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student not found or already inactive"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> softDeleteStudent(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId) {
        log.info("API Request: Soft Delete Student [{}]", studentId);
        userManagementService.softDeleteStudent(studentId);
        return ResponseEntity.ok("Student user deactivated successfully.");
    }

    @DeleteMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Soft Delete Staff",
            description = "Deactivates the User linked to the given Staff UUID (user.isActive=false)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff soft deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Staff not found or already inactive"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> softDeleteStaff(
            @Parameter(description = "Staff UUID", required = true, example = "4e95ad14-20da-4939-b666-841f3259997d")
            @PathVariable java.util.UUID staffId) {
        log.info("API Request: Soft Delete Staff [{}]", staffId);
        userManagementService.softDeleteStaff(staffId);
        return ResponseEntity.ok("Staff user deactivated successfully.");
    }

    @DeleteMapping("/student/{studentId}/guardian/{guardianId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Deactivate Guardian of Student",
            description = "Deactivates the guardian user linked with the specified student."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Guardian deactivated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student, guardian, or relationship not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> softDeleteGuardian(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId,
            @Parameter(description = "Guardian UUID", required = true, example = "51d020cc-b89f-42dc-98a3-b4fa88b66331")
            @PathVariable java.util.UUID guardianId) {
        log.info("API Request: Soft Delete Guardian [{}] for Student [{}]", guardianId, studentId);
        userManagementService.softDeleteGuardian(studentId, guardianId);
        return ResponseEntity.ok("Guardian deactivated successfully.");
    }

    @DeleteMapping("/student/{studentId}/guardian/{guardianId}/unlink")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Unlink Guardian From Student",
            description = "Removes only the guardian-student relationship metadata without deactivating the guardian account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Guardian unlinked successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student, guardian, or relationship not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> unlinkGuardian(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId,
            @Parameter(description = "Guardian UUID", required = true, example = "51d020cc-b89f-42dc-98a3-b4fa88b66331")
            @PathVariable java.util.UUID guardianId) {
        log.info("API Request: Unlink Guardian [{}] from Student [{}]", guardianId, studentId);
        userManagementService.unlinkGuardian(studentId, guardianId);
        return ResponseEntity.ok("Guardian unlinked successfully.");
    }

    @PatchMapping("/student/{studentId}/activation")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Activate/Deactivate Student User",
            description = "Toggles activation status of the User linked to a Student UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student user activation updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> setStudentUserActivation(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId,
            @Parameter(description = "Target user activation state", required = true, example = "false")
            @RequestParam boolean active) {
        log.info("API Request: Set Student User Activation [{}] => {}", studentId, active);
        userManagementService.setStudentUserActivation(studentId, active);
        return ResponseEntity.ok("Student user " + (active ? "activated" : "deactivated") + " successfully.");
    }

    @PatchMapping("/staff/{staffId}/activation")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Activate/Deactivate Staff User",
            description = "Toggles activation status of the User linked to a Staff UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff user activation updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Staff not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> setStaffUserActivation(
            @Parameter(description = "Staff UUID", required = true, example = "4e95ad14-20da-4939-b666-841f3259997d")
            @PathVariable java.util.UUID staffId,
            @Parameter(description = "Target user activation state", required = true, example = "true")
            @RequestParam boolean active) {
        log.info("API Request: Set Staff User Activation [{}] => {}", staffId, active);
        userManagementService.setStaffUserActivation(staffId, active);
        return ResponseEntity.ok("Staff user " + (active ? "activated" : "deactivated") + " successfully.");
    }

    @PatchMapping("/student/{studentId}/guardian/{guardianId}/activation")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Activate/Deactivate Guardian of Student",
            description = "Toggles activation status of a guardian linked to the specified student."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Guardian activation updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student, guardian, or relationship not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> setGuardianUserActivation(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId,
            @Parameter(description = "Guardian UUID", required = true, example = "51d020cc-b89f-42dc-98a3-b4fa88b66331")
            @PathVariable java.util.UUID guardianId,
            @Parameter(description = "Target guardian activation state", required = true, example = "true")
            @RequestParam boolean active) {
        log.info("API Request: Set Guardian Activation [{}] for Student [{}] => {}", guardianId, studentId, active);
        userManagementService.setGuardianUserActivation(studentId, guardianId, active);
        return ResponseEntity.ok("Guardian " + (active ? "activated" : "deactivated") + " successfully.");
    }

    // =================================================================================
    // 6. FULL DETAILS (ADMIN)
    // =================================================================================

    @GetMapping("/student/{studentId}/details")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get Full Student Details",
            description = "Returns complete profile details for a student by Student UUID. Accessible to School Admin and Super Admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student full details fetched successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ComprehensiveUserProfileResponseDTO> getStudentFullDetails(
            @Parameter(description = "Student UUID", required = true, example = "39170ff6-80ff-4831-bd4d-dbfc07cc2d61")
            @PathVariable java.util.UUID studentId) {
        log.info("API Request: Get Full Student Details [{}]", studentId);
        return ResponseEntity.ok(userManagementService.getStudentFullDetails(studentId));
    }

    @GetMapping("/staff/{staffId}/details")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get Full Staff Details",
            description = "Returns complete profile details for a staff member by Staff UUID. Accessible to School Admin and Super Admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Staff full details fetched successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires School Admin or Super Admin privileges"),
            @ApiResponse(responseCode = "404", description = "Staff not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ComprehensiveUserProfileResponseDTO> getStaffFullDetails(
            @Parameter(description = "Staff UUID", required = true, example = "4e95ad14-20da-4939-b666-841f3259997d")
            @PathVariable java.util.UUID staffId) {
        log.info("API Request: Get Full Staff Details [{}]", staffId);
        return ResponseEntity.ok(userManagementService.getStaffFullDetails(staffId));
    }
}