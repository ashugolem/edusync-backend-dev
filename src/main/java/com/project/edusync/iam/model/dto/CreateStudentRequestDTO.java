package com.project.edusync.iam.model.dto;

import com.project.edusync.uis.model.enums.Ethnicity;
import com.project.edusync.uis.model.enums.Gender;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateStudentRequestDTO extends CreateUserRequestDTO {

    // --- Core Enrollment Data ---
    private String enrollmentNumber;

    // --- NEW FIELD ---
    @NotNull(message = "Roll Number is required")
    private Integer rollNo;
    // ----------------

    @NotNull(message = "Section ID is required for enrollment")
    private UUID sectionId;

    private LocalDate enrollmentDate;

    // --- Demographics ---
    private Gender gender;

    private Ethnicity ethnicity;

    @Size(max = 100)
    private String nationality;

    private String languagePrimary;
    private String languageSecondary;

    private String homeEnvironmentDetails;

    // --- Medical Record ---
    private String primaryCarePhysician;
    private String physicianPhone;
    private String insuranceProvider;
    private String insurancePolicyNumber;

    private List<String> initialAllergies;
}