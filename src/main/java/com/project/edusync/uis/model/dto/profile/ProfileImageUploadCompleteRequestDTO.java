package com.project.edusync.uis.model.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileImageUploadCompleteRequestDTO {

    @NotBlank(message = "objectKey is required")
    private String objectKey;

    @NotBlank(message = "secureUrl is required")
    private String secureUrl;

    private String etag;
    private String metadata;
}

