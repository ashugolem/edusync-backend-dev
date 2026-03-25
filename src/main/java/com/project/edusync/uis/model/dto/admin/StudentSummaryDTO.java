package com.project.edusync.uis.model.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDate;

/**
 * A lightweight summary DTO for a Student — used in paginated admin list views.
 * Intentionally excludes heavy nested objects (medical records, guardians)
 * to keep list responses fast and clean.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentSummaryDTO {

    // --- Identity ---
    private Long studentId;
    private String uuid;
    private String enrollmentNumber;
    private String enrollmentStatus; // "ACTIVE" | "INACTIVE"

    // --- Personal Info (from UserProfile) ---
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String username;
    private String profileUrl;
    private LocalDate dateOfBirth;
    private String gender;

    // --- Academic Info ---
    private Integer rollNo;
    private LocalDate enrollmentDate;
    private String className;
    private String sectionName;
}






