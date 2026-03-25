package com.project.edusync.uis.model.dto.profile;

import com.project.edusync.uis.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {
    private Long profileId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String preferredName;
    private String profileUrl;
    private LocalDate dateOfBirth;
    private String bio;
    private Gender gender;
    // Aggregated fields from the User entity
    private String username;
    private String email;
}
