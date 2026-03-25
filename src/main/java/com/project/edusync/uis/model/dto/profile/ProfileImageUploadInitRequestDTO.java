package com.project.edusync.uis.model.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProfileImageUploadInitRequestDTO {

    @NotBlank(message = "fileName is required")
    private String fileName;

    @NotBlank(message = "contentType is required")
    private String contentType;

    @NotNull(message = "sizeBytes is required")
    @Positive(message = "sizeBytes must be greater than 0")
    private Long sizeBytes;
}

