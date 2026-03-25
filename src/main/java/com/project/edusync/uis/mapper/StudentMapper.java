package com.project.edusync.uis.mapper;

import com.project.edusync.iam.model.dto.CreateStudentRequestDTO;
import com.project.edusync.uis.model.dto.profile.StudentProfileDTO;
import com.project.edusync.uis.model.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for Student entity to StudentProfileDTO.
 * <p>
 * Handles the conversion of boolean flags to readable status strings
 * and maps the specific enrollment data.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface StudentMapper {

    // --- Core Student ---
    @Mapping(target = "enrollmentNumber", source = "enrollmentNumber")
    @Mapping(target = "enrollmentDate", source = "enrollmentDate")
    @Mapping(target = "rollNo", source = "rollNo")
    @Mapping(target = "active", constant = "true")
    Student toStudentEntity(CreateStudentRequestDTO dto);
    /**
     * Converts the Student entity to a profile DTO.
     *
     * @param student The source Student entity.
     * @return The profile DTO with enrollment details.
     */
    @Mapping(source = "id", target = "studentId")
    @Mapping(source = "enrollmentNumber", target = "enrollmentNo")
    @Mapping(source = "userProfile.profileUrl", target = "profileUrl")
    // Converts the boolean 'isActive' flag to a readable String for the frontend
    @Mapping(target = "enrollmentStatus", expression = "java(student.isActive() ? \"ACTIVE\" : \"INACTIVE\")")
    // Medical record is a complex nested object; we ignore it here to keep the fetch lightweight
    // unless explicitly loaded in a separate service call or expanded logic.
    @Mapping(target = "medicalRecord", ignore = true)
    StudentProfileDTO toDto(Student student);
}