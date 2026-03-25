package com.project.edusync.uis.model.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.edusync.uis.model.enums.StaffType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StaffProfileDTO {

    // --- Identity ---
    private Long staffId;
    private String staffSystemId; // Maps to Staff.uuid
    private String profileUrl;

    // --- Professional Info ---
    private String jobTitle;
    private String department; // Included from your snippet (Note: ensure this maps to a real column or relationship)
    private StaffType staffType; // TEACHER, PRINCIPAL, LIBRARIAN, etc.

    // --- Employment Details ---
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private String officeLocation;
    private boolean isActive;

    // --- Reporting Line ---
    private Long managerId;
    private String managerName; // Helper field populated by Mapper

    // --- Conditional Role-Specific Details ---
    // These fields are populated only if the staffType matches
    private TeacherDetailsDTO teacherDetails;
    private PrincipalDetailsDTO principalDetails;

    // Future placeholders for other staff types
    // private LibrarianDetailsDTO librarianDetails;
    // private SecurityGuardDetailsDTO securityGuardDetails;
}