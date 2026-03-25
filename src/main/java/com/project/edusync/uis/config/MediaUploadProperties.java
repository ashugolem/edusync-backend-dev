package com.project.edusync.uis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.media")
public class MediaUploadProperties {

    private String provider = "cloudinary";
    private long maxFileSizeBytes = 5_242_880L; // 5 MB default
    private long uploadInitTtlSeconds = 300L;
    private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
    private List<String> allowedSecureUrlPrefixes = List.of();

    private Cloudinary cloudinary = new Cloudinary();
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private String folder = "profiles";
    }

    @Getter
    @Setter
    public static class S3 {
        private String uploadUrlTemplate;
    }
}

