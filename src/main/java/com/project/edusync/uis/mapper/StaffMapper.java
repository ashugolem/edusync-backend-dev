package com.project.edusync.uis.mapper;

import com.project.edusync.iam.model.dto.BaseStaffRequestDTO;
import com.project.edusync.uis.model.dto.profile.PrincipalDetailsDTO;
import com.project.edusync.uis.model.dto.profile.StaffProfileDTO;
import com.project.edusync.uis.model.dto.profile.TeacherDetailsDTO;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.details.PrincipalDetails;
import com.project.edusync.uis.model.entity.details.TeacherDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Comprehensive Mapper for Staff members.
 * <p>
 * This interface handles the base {@link Staff} entity and provides specific
 * mapping methods for the specialized extension tables (TeacherDetails, PrincipalDetails).
 * </p>
 */
@Mapper(componentModel = "spring", uses = {JsonMapper.class})
public interface StaffMapper {

    @Mapping(target = "jobTitle", source = "jobTitle")
    @Mapping(target = "staffType", source = "staffType")
    @Mapping(target = "hireDate", source = "hireDate")
    @Mapping(target = "officeLocation", source = "officeLocation")
    @Mapping(target = "active", constant = "true")
    Staff toEntity(BaseStaffRequestDTO dto);

    /**
     * Maps the base Staff information common to all employees.
     *
     * @param staff The generic staff entity.
     * @return Base staff profile DTO.
     */
    @Mapping(source = "id", target = "staffId")
    // We assume the UUID (external ID) acts as the system ID.
    @Mapping(source = "uuid", target = "staffSystemId")
    @Mapping(source = "userProfile.profileUrl", target = "profileUrl")
    // 'jobTitle' and 'staffType' map automatically by name
    StaffProfileDTO toDto(Staff staff);

    /**
     * Maps specific details for Teaching staff.
     */
    TeacherDetailsDTO toTeacherDto(TeacherDetails details);

    /**
     * Maps specific details for Principal/Admin staff.
     */
    PrincipalDetailsDTO toPrincipalDto(PrincipalDetails details);
}