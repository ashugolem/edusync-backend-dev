package com.project.edusync.uis.service;

import com.project.edusync.uis.model.dto.admin.StaffSummaryDTO;
import com.project.edusync.uis.model.dto.admin.StudentSummaryDTO;
import com.project.edusync.uis.model.enums.StaffType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for admin-level querying of Students and Staff.
 * <p>
 * Restricted to SCHOOL_ADMIN and SUPER_ADMIN roles.
 * Provides paginated, searchable, and filterable list views.
 * </p>
 */
public interface AdminUserQueryService {

    /**
     * Returns a paginated list of all students.
     *
     * @param search   Optional search keyword (name, email, enrollmentNumber). Pass null/blank to skip.
     * @param active   Optional filter by linked user activation status. Pass null to fetch both.
     * @param pageable Pagination and sorting parameters.
     * @return A page of {@link StudentSummaryDTO}.
     */
    Page<StudentSummaryDTO> getAllStudents(String search, Boolean active, Pageable pageable);

    /**
     * Returns a paginated list of all staff members.
     *
     * @param search    Optional search keyword (name, email, employeeId, jobTitle). Pass null/blank to skip.
     * @param staffType Optional filter by staff type (TEACHER, PRINCIPAL, etc.). Pass null to fetch all.
     * @param active    Optional filter by linked user activation status. Pass null to fetch both.
     * @param pageable  Pagination and sorting parameters.
     * @return A page of {@link StaffSummaryDTO}.
     */
    Page<StaffSummaryDTO> getAllStaff(String search, StaffType staffType, Boolean active, Pageable pageable);
}

