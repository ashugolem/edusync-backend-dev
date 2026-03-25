package com.project.edusync.uis.model.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserProfileResponseDTO {
    // --- From UserProfile (and AuditableEntity) ---
    private Long id; // The UserProfile ID
    private String uuid;
    private String firstName;
    private String middleName;
    private String lastName;
    private String preferredName;
    private String profileUrl;
    private LocalDate dateOfBirth;
    private String bio;

    // --- Aggregated from User entity ---
    private String username;
    private String email;

    // --- Audit fields (often useful for display) ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
