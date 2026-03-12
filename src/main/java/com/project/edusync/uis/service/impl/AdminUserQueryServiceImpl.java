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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link AdminUserQueryService}.
 * <p>
 * Maps raw entity pages to lightweight summary DTOs, keeping list responses
 * fast by avoiding deep nesting (medical records, certifications, etc.).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserQueryServiceImpl implements AdminUserQueryService {

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    // =========================================================================
    // STUDENTS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<StudentSummaryDTO> getAllStudents(String search, Pageable pageable) {
        log.info("Admin query: getAllStudents | search='{}' | page={} | size={}",
                search, pageable.getPageNumber(), pageable.getPageSize());

        Page<Student> studentPage;

        if (StringUtils.hasText(search)) {
            studentPage = studentRepository.searchStudents(search.trim(), pageable);
        } else {
            studentPage = studentRepository.findAllWithDetails(pageable);
        }

        return studentPage.map(this::toStudentSummaryDTO);
    }

    // =========================================================================
    // STAFF
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<StaffSummaryDTO> getAllStaff(String search, StaffType staffType, Pageable pageable) {
        log.info("Admin query: getAllStaff | search='{}' | staffType='{}' | page={} | size={}",
                search, staffType, pageable.getPageNumber(), pageable.getPageSize());

        Page<Staff> staffPage;

        if (StringUtils.hasText(search)) {
            // Search takes priority; staffType filter is ignored when searching
            staffPage = staffRepository.searchStaff(search.trim(), pageable);
        } else if (staffType != null) {
            staffPage = staffRepository.findAllByStaffTypeWithDetails(staffType, pageable);
        } else {
            staffPage = staffRepository.findAllWithDetails(pageable);
        }

        return staffPage.map(this::toStaffSummaryDTO);
    }

    // =========================================================================
    // PRIVATE MAPPERS
    // =========================================================================

    /**
     * Maps a {@link Student} entity (with eagerly loaded associations)
     * to a {@link StudentSummaryDTO}.
     */
    private StudentSummaryDTO toStudentSummaryDTO(Student student) {
        UserProfile profile = student.getUserProfile();

        return StudentSummaryDTO.builder()
                .studentId(student.getId())
                .uuid(student.getUuid() != null ? student.getUuid().toString() : null)
                .enrollmentNumber(student.getEnrollmentNumber())
                .enrollmentStatus(student.isActive() ? "ACTIVE" : "INACTIVE")
                // Personal info
                .firstName(profile.getFirstName())
                .middleName(profile.getMiddleName())
                .lastName(profile.getLastName())
                .email(profile.getUser() != null ? profile.getUser().getEmail() : null)
                .username(profile.getUser() != null ? profile.getUser().getUsername() : null)
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                // Academic info
                .rollNo(student.getRollNo())
                .enrollmentDate(student.getEnrollmentDate())
                .className(student.getSection().getAcademicClass().getName())
                .sectionName(student.getSection().getSectionName())
                .build();
    }

    /**
     * Maps a {@link Staff} entity (with eagerly loaded associations)
     * to a {@link StaffSummaryDTO}.
     */
    private StaffSummaryDTO toStaffSummaryDTO(Staff staff) {
        UserProfile profile = staff.getUserProfile();

        return StaffSummaryDTO.builder()
                .staffId(staff.getId())
                .uuid(staff.getUuid() != null ? staff.getUuid().toString() : null)
                .employeeId(staff.getEmployeeId())
                // Personal info
                .firstName(profile.getFirstName())
                .middleName(profile.getMiddleName())
                .lastName(profile.getLastName())
                .email(profile.getUser() != null ? profile.getUser().getEmail() : null)
                .username(profile.getUser() != null ? profile.getUser().getUsername() : null)
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                // Professional info
                .jobTitle(staff.getJobTitle())
                .department(staff.getDepartment() != null ? staff.getDepartment().name() : null)
                .staffType(staff.getStaffType())
                .hireDate(staff.getHireDate())
                .officeLocation(staff.getOfficeLocation())
                .active(staff.isActive())
                .build();
    }
}



