package com.project.edusync.iam.service;

import com.project.edusync.iam.model.dto.*;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.model.dto.profile.ComprehensiveUserProfileResponseDTO;

/**
 * Service interface for High-Level User Management.
 * <p>
 * Orchestrates the creation of complex user entities (Student, Staff, etc.), ensuring
 * data consistency across Identity, Profile, and Role-Specific tables.
 * </p>
 */
public interface UserManagementService {

    /**
     * Creates a School Admin user.
     * @param request DTO containing identity and profile details.
     * @return The created User entity.
     * @throws com.project.edusync.common.exception.iam.UserAlreadyExistsException if username/email exists.
     */
    User createSchoolAdmin(CreateUserRequestDTO request);

    /**
     * Creates a comprehensive Student record (User + Profile + Student + Demographics + Medical).
     * @param request The composite DTO with all enrollment data.
     * @return The created User entity.
     */
    User createStudent(CreateStudentRequestDTO request);

    /**
     * Creates a Teacher with specific academic details.
     * @param request DTO with teacher-specific fields (certifications, etc).
     * @return The created User entity.
     */
    User createTeacher(CreateTeacherRequestDTO request);

    /**
     * Creates a Principal with administrative details.
     * @param request DTO with principal-specific fields.
     * @return The created User entity.
     */
    User createPrincipal(CreatePrincipalRequestDTO request);

    /**
     * Creates a Librarian with system permissions.
     * @param request DTO with librarian-specific fields.
     * @return The created User entity.
     */
    User createLibrarian(CreateLibrarianRequestDTO request);

    /**
     * Updates an existing Student record and related profile/user data.
     * @param studentId The student's UUID.
     * @param request The update payload.
     * @return The updated User entity.
     */
    User updateStudent(java.util.UUID studentId, com.project.edusync.iam.model.dto.UpdateStudentRequestDTO request);

    /**
     * Updates an existing Staff record and related profile/user data.
     * @param staffId The staff's UUID.
     * @param request The update payload.
     * @return The updated User entity.
     */
    User updateStaff(java.util.UUID staffId, com.project.edusync.iam.model.dto.UpdateStaffRequestDTO request);

    /**
     * Soft deletes a Student by UUID by marking isActive=false.
     * @param studentId Student UUID.
     */
    void softDeleteStudent(java.util.UUID studentId);

    /**
     * Soft deletes a Staff by UUID by marking isActive=false.
     * @param staffId Staff UUID.
     */
    void softDeleteStaff(java.util.UUID staffId);

    /**
     * Activates or deactivates the User linked to a Student entity.
     * @param studentId Student UUID.
     * @param active Target activation state for the linked user.
     */
    void setStudentUserActivation(java.util.UUID studentId, boolean active);

    /**
     * Activates or deactivates the User linked to a Staff entity.
     * @param staffId Staff UUID.
     * @param active Target activation state for the linked user.
     */
    void setStaffUserActivation(java.util.UUID staffId, boolean active);

    /**
     * Returns complete profile details for a Student identified by student UUID.
     * @param studentId Student UUID.
     * @return Full profile response including student/staff/guardian facets when applicable.
     */
    ComprehensiveUserProfileResponseDTO getStudentFullDetails(java.util.UUID studentId);

    /**
     * Returns complete profile details for a Staff member identified by staff UUID.
     * @param staffId Staff UUID.
     * @return Full profile response including student/staff/guardian facets when applicable.
     */
    ComprehensiveUserProfileResponseDTO getStaffFullDetails(java.util.UUID staffId);
}