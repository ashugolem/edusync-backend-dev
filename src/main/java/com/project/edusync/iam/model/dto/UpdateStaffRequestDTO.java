package com.project.edusync.iam.model.dto;

import com.project.edusync.uis.model.enums.Department;
import com.project.edusync.uis.model.enums.Gender;
import com.project.edusync.uis.model.enums.StaffType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateStaffRequestDTO {

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

    // --- Staff ---
    private String employeeId;
    private String jobTitle;
    private LocalDate hireDate;
    private String officeLocation;
    private Department department;
    private StaffType staffType;
}

