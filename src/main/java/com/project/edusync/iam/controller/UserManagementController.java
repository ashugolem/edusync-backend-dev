package com.project.edusync.iam.controller;

import com.project.edusync.iam.model.dto.*;
import com.project.edusync.iam.service.UserManagementService;
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

    // =================================================================================
    // 5. SOFT DELETE STUDENT / STAFF
    // =================================================================================

    @DeleteMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Soft Delete Student",
            description = "Marks the student as inactive (isActive=false). Inactive students are excluded from listing APIs."
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
        return ResponseEntity.ok("Student soft deleted successfully.");
    }

    @DeleteMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Soft Delete Staff",
            description = "Marks the staff member as inactive (isActive=false). Inactive staff are excluded from listing APIs."
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
        return ResponseEntity.ok("Staff soft deleted successfully.");
    }
}