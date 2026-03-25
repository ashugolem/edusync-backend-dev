package com.project.edusync.uis.model.dto.profile;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ProfileImageUploadInitResponseDTO {
    private String provider;
    private String method;
    private String uploadUrl;
    private String objectKey;
    private Instant expiresAt;
    private Map<String, String> fields;
    private Map<String, String> headers;
}

