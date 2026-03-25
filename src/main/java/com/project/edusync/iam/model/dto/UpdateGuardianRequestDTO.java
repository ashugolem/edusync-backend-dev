package com.project.edusync.iam.model.dto;

import com.project.edusync.uis.model.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateGuardianRequestDTO {

    @Email(message = "Invalid email format")
    private String email;

    private String firstName;
    private String middleName;
    private String lastName;
    private String preferredName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 5000, message = "Bio is too long")
    private String bio;

    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;

    @Size(max = 100, message = "Occupation must be at most 100 characters")
    private String occupation;

    @Size(max = 100, message = "Employer must be at most 100 characters")
    private String employer;

    @Size(max = 50, message = "Relationship type must be at most 50 characters")
    private String relationshipType;

    private Boolean primaryContact;
    private Boolean canPickup;
    private Boolean financialContact;
    private Boolean canViewGrades;
}

