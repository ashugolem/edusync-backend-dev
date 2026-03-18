package com.project.edusync.iam.model.dto;

import com.project.edusync.uis.model.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateStudentRequestDTO {

    // --- Identity (User) ---
    @Email(message = "Invalid email format")
    private String email;

    // --- Personal Profile ---
    private String firstName;
    private String middleName;
    private String lastName;
    private String preferredName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 5000, message = "Bio is too long")
    private String bio;

    // --- Enrollment ---
    private String enrollmentNumber;
    private Integer rollNo;
    private UUID sectionId;
    private LocalDate enrollmentDate;
}

