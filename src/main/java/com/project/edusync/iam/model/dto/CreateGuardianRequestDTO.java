package com.project.edusync.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateGuardianRequestDTO extends CreateUserRequestDTO {

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;

    @Size(max = 100, message = "Occupation must be at most 100 characters")
    private String occupation;

    @Size(max = 100, message = "Employer must be at most 100 characters")
    private String employer;

    @NotBlank(message = "Relationship type is required")
    @Size(max = 50, message = "Relationship type must be at most 50 characters")
    private String relationshipType;

    private boolean primaryContact;
    private boolean canPickup;
    private boolean financialContact;
    private boolean canViewGrades;
}

