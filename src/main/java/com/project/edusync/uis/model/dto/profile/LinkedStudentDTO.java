package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkedStudentDTO {
    private UUID studentUuid;
    private String studentName;
    private String profileUrl;
    private String enrollmentNo;
    private String relationshipType;
}
