package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuardianProfileDTO {
    private UUID guardianUuid;
    private String name;
    private String relation;
    private String profileUrl;
    private String occupation;
    private String employer;
    // ... other fields from Guardian.java

    // This list will show the students this guardian is responsible for
    private List<LinkedStudentDTO> linkedStudents;
}
