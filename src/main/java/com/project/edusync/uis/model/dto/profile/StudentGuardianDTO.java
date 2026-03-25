package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentGuardianDTO {
    private UUID guardianUuid;
    private String name;
    private String relation;
    private String profileUrl;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String occupation;
    private String employer;
    private boolean primaryContact;
    private boolean canPickup;
    private boolean financialContact;
    private boolean canViewGrades;
    private boolean active;
}

