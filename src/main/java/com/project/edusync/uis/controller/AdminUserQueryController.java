package com.project.edusync.uis.controller;

import com.project.edusync.uis.model.dto.admin.StaffSummaryDTO;
import com.project.edusync.uis.model.dto.admin.StudentSummaryDTO;
import com.project.edusync.uis.model.enums.StaffType;
import com.project.edusync.uis.service.AdminUserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for admin-level user listing (Students & Staff).
 * <p>
 * All endpoints are restricted to SCHOOL_ADMIN and SUPER_ADMIN roles.
 * Supports pagination, sorting, search, and (for staff) filtering by StaffType.
 * </p>
 *
 * <pre>
 *   Base URL: /api/v1/auth/admin/users
 *
 *   GET /students            → List all students (paginated + searchable)
 *   GET /staff               → List all staff   (paginated + searchable + filterable by type)
 * </pre>
 */
@RestController
@RequestMapping("${api.url}/auth/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin User Management", description = "Secure endpoints for creating and enrolling users (Students & Staff)")
public class AdminUserQueryController {

    private final AdminUserQueryService adminUserQueryService;

    // =========================================================================
    // GET ALL STUDENTS
    // =========================================================================

    /**
     * Retrieves a paginated list of all students in the system.
     *
     * <p><b>Access:</b> SCHOOL_ADMIN, SUPER_ADMIN</p>
     *
     * <p><b>Query Params:</b></p>
     * <ul>
     *   <li>{@code search}  – optional keyword to filter by name, email, or enrollment number</li>
     *   <li>{@code active}  – optional filter by linked user status (true = active, false = inactive)</li>
     *   <li>{@code page}    – zero-based page index (default: 0)</li>
     *   <li>{@code size}    – number of records per page (default: 20, max: 100)</li>
     *   <li>{@code sortBy}  – field to sort by (default: "enrollmentNumber")</li>
     *   <li>{@code sortDir} – "asc" or "desc" (default: "asc")</li>
     * </ul>
     */
    @GetMapping("/students")
    @PreAuthorize("hasAnyAuthority('ROLE_SCHOOL_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(
            summary = "Get All Students",
            description = "Returns a paginated, searchable list of all students. " +
                          "Restricted to School Admin and Super Admin."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student list retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden – requires SCHOOL_ADMIN or SUPER_ADMIN role")
    })
    public ResponseEntity<Page<StudentSummaryDTO>> getAllStudents(
            @Parameter(description = "Search by name, email, or enrollment number")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by linked user status: true (active), false (inactive)")
            @RequestParam(required = false) Boolean active,

            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of records per page (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Field to sort by (default: enrollmentNumber)")
            @RequestParam(defaultValue = "enrollmentNumber") String sortBy,

            @Parameter(description = "Sort direction: asc or desc (default: asc)")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        size = Math.min(size, 100); // hard cap at 100 per page
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        log.info("Admin API: GET /students | search='{}' | active='{}' | page={} | size={} | sort={}",
                search, active, page, size, sortBy + " " + sortDir);

        Page<StudentSummaryDTO> result = adminUserQueryService.getAllStudents(search, active, pageable);
        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // GET ALL STAFF
    // =========================================================================

    /**
     * Retrieves a paginated list of all staff members in the system.
     *
     * <p><b>Access:</b> SCHOOL_ADMIN, SUPER_ADMIN</p>
     *
     * <p><b>Query Params:</b></p>
     * <ul>
     *   <li>{@code search}    – optional keyword to filter by name, email, employeeId, or jobTitle</li>
     *   <li>{@code staffType} – optional filter: TEACHER | PRINCIPAL | LIBRARIAN | etc.</li>
     *   <li>{@code active}    – optional filter by linked user status (true = active, false = inactive)</li>
     *   <li>{@code page}      – zero-based page index (default: 0)</li>
     *   <li>{@code size}      – number of records per page (default: 20, max: 100)</li>
     *   <li>{@code sortBy}    – field to sort by (default: "employeeId")</li>
     *   <li>{@code sortDir}   – "asc" or "desc" (default: "asc")</li>
     * </ul>
     */
    @GetMapping("/staff")
    @PreAuthorize("hasAnyAuthority('ROLE_SCHOOL_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(
            summary = "Get All Staff",
            description = "Returns a paginated, searchable, and filterable list of all staff members. " +
                          "Optionally filter by staffType (TEACHER, PRINCIPAL, LIBRARIAN, etc.). " +
                          "Restricted to School Admin and Super Admin."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff list retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden – requires SCHOOL_ADMIN or SUPER_ADMIN role")
    })
    public ResponseEntity<Page<StaffSummaryDTO>> getAllStaff(
            @Parameter(description = "Search by name, email, employee ID, or job title")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by staff type: TEACHER, PRINCIPAL, LIBRARIAN, etc.")
            @RequestParam(required = false) StaffType staffType,

            @Parameter(description = "Filter by linked user status: true (active), false (inactive)")
            @RequestParam(required = false) Boolean active,

            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of records per page (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Field to sort by (default: employeeId)")
            @RequestParam(defaultValue = "employeeId") String sortBy,

            @Parameter(description = "Sort direction: asc or desc (default: asc)")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        size = Math.min(size, 100); // hard cap at 100 per page
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        log.info("Admin API: GET /staff | search='{}' | staffType='{}' | active='{}' | page={} | size={} | sort={}",
                search, staffType, active, page, size, sortBy + " " + sortDir);

        Page<StaffSummaryDTO> result = adminUserQueryService.getAllStaff(search, staffType, active, pageable);
        return ResponseEntity.ok(result);
    }
}

