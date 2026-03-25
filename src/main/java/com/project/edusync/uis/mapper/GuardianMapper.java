package com.project.edusync.uis.mapper;

import com.project.edusync.uis.model.dto.profile.GuardianProfileDTO;
import com.project.edusync.uis.model.dto.profile.LinkedStudentDTO;
import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.StudentGuardianRelationship;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for Guardian profiles and their relationships to students.
 */
@Mapper(componentModel = "spring")
public interface GuardianMapper {

    /**
     * Converts a Guardian entity to DTO.
     * MapStruct automatically iterates over the 'studentRelationships' set
     * and uses the 'toLinkedStudentDto' method for each item.
     */
    @Mapping(source = "uuid", target = "guardianUuid")
    @Mapping(source = "userProfile.profileUrl", target = "profileUrl")
    @Mapping(source = "studentRelationships", target = "linkedStudents")
    GuardianProfileDTO toDto(Guardian guardian);

    /**
     * Converts a relationship record into a summary of the Student.
     * <p>
     * <b>Note on 'studentName':</b> The Student entity doesn't have a name;
     * it links to a UserProfile. We use a custom Java expression/helper
     * to traverse this graph safely.
     * </p>
     */
    @Mapping(source = "student.uuid", target = "studentUuid")
    @Mapping(source = "student.userProfile.profileUrl", target = "profileUrl")
    @Mapping(source = "student.enrollmentNumber", target = "enrollmentNo")
    @Mapping(target = "studentName", expression = "java(getStudentFullName(relationship))")
    @Mapping(source = "relationshipType", target = "relationshipType")
    LinkedStudentDTO toLinkedStudentDto(StudentGuardianRelationship relationship);

    /**
     * Helper method to safely extract a student's full name from their linked UserProfile.
     * MapStruct calls this method for the 'studentName' mapping above.
     */
    default String getStudentFullName(StudentGuardianRelationship relationship) {
        if (relationship.getStudent() != null && relationship.getStudent().getUserProfile() != null) {
            return relationship.getStudent().getUserProfile().getFirstName() + " " +
                    relationship.getStudent().getUserProfile().getLastName();
        }
        return "Unknown";
    }
}