package com.project.edusync.uis.service.impl;

import com.project.edusync.uis.model.dto.admin.StaffSummaryDTO;
import com.project.edusync.uis.model.dto.admin.StudentSummaryDTO;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.enums.StaffType;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.service.AdminUserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AdminUserQueryService}.
 *
 * <p><b>Sort field resolution:</b> Because our JPQL queries use JOIN FETCH,
 * Spring Data appends ORDER BY against the root entity alias (e.g. {@code s.firstName}).
 * Fields like {@code firstName} live on the joined {@code UserProfile}, not on
 * {@code Student} directly. We maintain a whitelist that maps user-facing sort
 * field names to their correct JPQL paths (e.g. "firstName" → "userProfile.firstName"),
 * falling back to a safe default when an unknown field is provided.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserQueryServiceImpl implements AdminUserQueryService {

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    /**
     * Maps user-facing sort field names to valid JPQL paths on the Student entity graph.
     * Any field not in this map is rejected and replaced with the safe default.
     */
    private static final Map<String, String> STUDENT_SORT_FIELDS = Map.of(
            "enrollmentNumber",  "enrollmentNumber",
            "enrollmentDate",    "enrollmentDate",
            "rollNo",            "rollNo",
            "firstName",         "userProfile.firstName",
            "lastName",          "userProfile.lastName",
            "email",             "userProfile.user.email"
    );
    private static final String STUDENT_DEFAULT_SORT = "enrollmentNumber";

    /**
     * Maps user-facing sort field names to valid JPQL paths on the Staff entity graph.
     */
    private static final Map<String, String> STAFF_SORT_FIELDS = Map.of(
            "employeeId",   "employeeId",
            "hireDate",     "hireDate",
            "jobTitle",     "jobTitle",
            "firstName",    "userProfile.firstName",
            "lastName",     "userProfile.lastName",
            "email",        "userProfile.user.email"
    );
    private static final String STAFF_DEFAULT_SORT = "employeeId";

    // =========================================================================
    // STUDENTS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<StudentSummaryDTO> getAllStudents(String search, Boolean active, Pageable pageable) {
        Pageable resolved = resolveSortFields(pageable, STUDENT_SORT_FIELDS, STUDENT_DEFAULT_SORT);

        log.info("Admin query: getAllStudents | search='{}' | active='{}' | page={} | size={}",
                search, active, resolved.getPageNumber(), resolved.getPageSize());

        Page<Student> studentPage = StringUtils.hasText(search)
                ? studentRepository.searchStudents(search.trim(), active, resolved)
                : studentRepository.findAllWithDetails(active, resolved);

        return studentPage.map(this::toStudentSummaryDTO);
    }

    // =========================================================================
    // STAFF
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<StaffSummaryDTO> getAllStaff(String search, StaffType staffType, Boolean active, Pageable pageable) {
        Pageable resolved = resolveSortFields(pageable, STAFF_SORT_FIELDS, STAFF_DEFAULT_SORT);

        log.info("Admin query: getAllStaff | search='{}' | staffType='{}' | active='{}' | page={} | size={}",
                search, staffType, active, resolved.getPageNumber(), resolved.getPageSize());

        Page<Staff> staffPage;
        if (StringUtils.hasText(search)) {
            staffPage = staffRepository.searchStaff(search.trim(), active, resolved);
        } else if (staffType != null) {
            staffPage = staffRepository.findAllByStaffTypeWithDetails(staffType, active, resolved);
        } else {
            staffPage = staffRepository.findAllWithDetails(active, resolved);
        }

        return staffPage.map(this::toStaffSummaryDTO);
    }

    // =========================================================================
    // SORT FIELD RESOLVER
    // =========================================================================

    /**
     * Translates each sort property in the incoming {@link Pageable} to the
     * correct JPQL path using the provided whitelist. Unknown fields fall back
     * to {@code defaultField}.
     *
     * <p>This prevents {@code UnknownPathException} when a user passes a sort
     * field like "firstName" which lives on a joined entity, not the root.</p>
     */
    private Pageable resolveSortFields(Pageable pageable, Map<String, String> whitelist, String defaultField) {
        Sort resolvedSort;

        if (!pageable.getSort().isSorted()) {
            // No sort specified — apply the safe default ascending
            resolvedSort = Sort.by(defaultField).ascending();
        } else {
            List<Sort.Order> orders = pageable.getSort().stream()
                    .map(order -> {
                        String mapped = whitelist.getOrDefault(order.getProperty(), defaultField);
                        if (!mapped.equals(order.getProperty())) {
                            log.debug("Sort field '{}' resolved to JPQL path '{}'", order.getProperty(), mapped);
                        }
                        return order.isAscending()
                                ? Sort.Order.asc(mapped)
                                : Sort.Order.desc(mapped);
                    })
                    .collect(Collectors.toList());
            resolvedSort = Sort.by(orders);
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolvedSort);
    }

    // =========================================================================
    // PRIVATE MAPPERS
    // =========================================================================

    private StudentSummaryDTO toStudentSummaryDTO(Student student) {
        UserProfile profile = student.getUserProfile();
        boolean userActive = profile.getUser() != null && profile.getUser().isActive();
        return StudentSummaryDTO.builder()
                .studentId(student.getId())
                .uuid(student.getUuid() != null ? student.getUuid().toString() : null)
                .enrollmentNumber(student.getEnrollmentNumber())
                .enrollmentStatus(userActive ? "ACTIVE" : "INACTIVE")
                .firstName(profile.getFirstName())
                .middleName(profile.getMiddleName())
                .lastName(profile.getLastName())
                .email(profile.getUser() != null ? profile.getUser().getEmail() : null)
                .username(profile.getUser() != null ? profile.getUser().getUsername() : null)
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .rollNo(student.getRollNo())
                .enrollmentDate(student.getEnrollmentDate())
                .className(student.getSection().getAcademicClass().getName())
                .sectionName(student.getSection().getSectionName())
                .build();
    }

    private StaffSummaryDTO toStaffSummaryDTO(Staff staff) {
        UserProfile profile = staff.getUserProfile();
        boolean userActive = profile.getUser() != null && profile.getUser().isActive();
        return StaffSummaryDTO.builder()
                .staffId(staff.getId())
                .uuid(staff.getUuid() != null ? staff.getUuid().toString() : null)
                .employeeId(staff.getEmployeeId())
                .firstName(profile.getFirstName())
                .middleName(profile.getMiddleName())
                .lastName(profile.getLastName())
                .email(profile.getUser() != null ? profile.getUser().getEmail() : null)
                .username(profile.getUser() != null ? profile.getUser().getUsername() : null)
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .jobTitle(staff.getJobTitle())
                .department(staff.getDepartment() != null ? staff.getDepartment().name() : null)
                .staffType(staff.getStaffType())
                .hireDate(staff.getHireDate())
                .officeLocation(staff.getOfficeLocation())
                .active(userActive)
                .build();
    }
}


