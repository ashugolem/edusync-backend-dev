package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentGuardianDTO {
    private Long guardianId;
    private String guardianUuid;
    private String name;
    private String relation;
    private String phoneNumber;
    private String occupation;
    private String employer;
    private boolean primaryContact;
    private boolean canPickup;
    private boolean financialContact;
    private boolean canViewGrades;
    private boolean active;
}

