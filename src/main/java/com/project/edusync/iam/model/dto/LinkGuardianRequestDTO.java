package com.project.edusync.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class LinkGuardianRequestDTO {

    @NotNull(message = "Guardian UUID is required")
    private UUID guardianId;

    @NotBlank(message = "Relationship type is required")
    @Size(max = 50, message = "Relationship type must be at most 50 characters")
    private String relationshipType;

    private boolean primaryContact;
    private boolean canPickup;
    private boolean financialContact;
    private boolean canViewGrades;
}

