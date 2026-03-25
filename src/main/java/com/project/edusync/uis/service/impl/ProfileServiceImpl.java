package com.project.edusync.uis.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import com.project.edusync.uis.config.MediaUploadProperties;
import com.project.edusync.uis.mapper.*;
import com.project.edusync.uis.model.dto.profile.ComprehensiveUserProfileResponseDTO;
import com.project.edusync.uis.model.dto.profile.ProfileImageUploadCompleteRequestDTO;
import com.project.edusync.uis.model.dto.profile.ProfileImageUploadInitRequestDTO;
import com.project.edusync.uis.model.dto.profile.ProfileImageUploadInitResponseDTO;
import com.project.edusync.uis.model.dto.profile.StaffProfileDTO;
import com.project.edusync.uis.model.dto.profile.UserProfileDTO;
import com.project.edusync.uis.model.dto.profile.UserProfileUpdateDTO;
import com.project.edusync.uis.model.entity.UserAddress;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.enums.StaffType;
import com.project.edusync.uis.repository.*;
import com.project.edusync.uis.repository.details.PrincipalDetailsRepository;
import com.project.edusync.uis.repository.details.TeacherDetailsRepository;
import com.project.edusync.uis.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing User Profiles.
 * <p>
 * This service acts as an aggregator/orchestrator. Since EduSync uses a "Decoupled Identity" model
 * (User vs UserProfile) and a "One Person, Multiple Roles" model (Student, Staff, Guardian),
 * this service must query multiple repositories to construct the full "Comprehensive" profile.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    // --- Core Identity Repositories ---
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAddressRepository userAddressRepository;

    // --- Role-Specific Repositories ---
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final GuardianRepository guardianRepository;

    // --- Detailed Information Repositories (Extension Tables) ---
    private final TeacherDetailsRepository teacherDetailsRepository;
    private final PrincipalDetailsRepository principalDetailsRepository;

    // --- Mappers ---
    private final UserProfileMapper userProfileMapper;
    private final AddressMapper addressMapper;
    private final StudentMapper studentMapper;
    private final StaffMapper staffMapper;
    private final GuardianMapper guardianMapper;
    private final MediaUploadProperties mediaUploadProperties;

    /**
     * Retrieves the full profile for a user, including all their associated roles.
     * <p>
     * This method performs a "waterfall" of checks:
     * 1. Validate User (Identity)
     * 2. Fetch Profile (Personal Info)
     * 3. Fetch Addresses
     * 4. Check for Student Role
     * 5. Check for Staff Role (and sub-details like Teacher/Principal)
     * 6. Check for Guardian Role
     * </p>
     *
     * @param userId The unique ID from the IAM User table.
     * @return A DTO containing personal info, addresses, and any role-specific data.
     * @throws ResourceNotFoundException if the User or UserProfile does not exist.
     */
    @Override
    @Transactional(readOnly = true)
    public ComprehensiveUserProfileResponseDTO getProfileByUserId(Long userId) {
        log.info("Request received to fetch comprehensive profile for User ID: {}", userId);

        // 1. Fetch User (Identity)
        // casting to int because IAM User ID is Integer, while service layer uses Long for uniformity
        User user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> {
                    log.error("Profile fetch failed: IAM User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });

        // 2. Fetch UserProfile (Person Details)
        // This links the Login Account (User) to the Person (Profile)
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Profile fetch failed: UserProfile not found for User ID: {}", userId);
                    return new ResourceNotFoundException("UserProfile", "userId", userId);
                });

        ComprehensiveUserProfileResponseDTO response = new ComprehensiveUserProfileResponseDTO();

        // 3. Map Basic Info (Merge User + Profile data)
        log.debug("Mapping basic profile information for Profile ID: {}", profile.getId());
        response.setBasicProfile(userProfileMapper.toResponseDto(profile, user));

        // 4. Map Addresses
        List<UserAddress> addresses = userAddressRepository.findByUserProfile(profile);
        if (!addresses.isEmpty()) {
            log.debug("Found {} address(es) for Profile ID: {}", addresses.size(), profile.getId());
            response.setAddresses(addresses.stream()
                    .map(addressMapper::toDto)
                    .collect(Collectors.toList()));
        }

        // 5. Role Discovery & Mapping
        // We check each role repository to see if this Profile ID exists there.
        log.debug("Starting role discovery for Profile ID: {}", profile.getId());

        // -- Check: Is this user a Student? --
        studentRepository.findByUserProfile(profile).ifPresent(student -> {
            log.info("Role Detected: User [ID: {}] is a STUDENT. Student ID: {}", userId, student.getId());
            response.setStudentDetails(studentMapper.toDto(student));
        });

        // -- Check: Is this user Staff? --
        staffRepository.findByUserProfile(profile).ifPresent(staff -> {
            log.info("Role Detected: User [ID: {}] is STAFF. Staff ID: {}, Type: {}", userId, staff.getId(), staff.getStaffType());
            StaffProfileDTO staffDto = staffMapper.toDto(staff);

            // -- Deep Fetch: Get Specific Staff Details --
            // Based on the StaffType enum, we fetch the corresponding extension table (1:1 with Staff)
            try {
                if (StaffType.TEACHER.equals(staff.getStaffType())) {
                    // Optimized: Passed Long ID directly.
                    teacherDetailsRepository.findById(Integer.valueOf(staff.getId().toString()))
                            .ifPresentOrElse(
                                    td -> {
                                        log.debug("Fetched specialized Teacher details for Staff ID: {}", staff.getId());
                                        staffDto.setTeacherDetails(staffMapper.toTeacherDto(td));
                                    },
                                    () -> log.warn("Data Consistency Warning: Staff is marked as TEACHER but no entry found in TeacherDetails table for Staff ID: {}", staff.getId())
                            );
                } else if (StaffType.PRINCIPAL.equals(staff.getStaffType())) {
                    principalDetailsRepository.findById(Integer.valueOf(staff.getId().toString()))
                            .ifPresentOrElse(
                                    pd -> {
                                        log.debug("Fetched specialized Principal details for Staff ID: {}", staff.getId());
                                        staffDto.setPrincipalDetails(staffMapper.toPrincipalDto(pd));
                                    },
                                    () -> log.warn("Data Consistency Warning: Staff is marked as PRINCIPAL but no entry found in PrincipalDetails table for Staff ID: {}", staff.getId())
                            );
                }
            } catch (Exception e) {
                log.error("Error fetching staff subtype details for Staff ID: {}", staff.getId(), e);
                // We do not throw here to allow partial profile loading (e.g. return basic staff info even if details fail)
            }

            response.setStaffDetails(staffDto);
        });

        // -- Check: Is this user a Guardian? --
        guardianRepository.findByUserProfile(profile).ifPresent(guardian -> {
            log.info("Role Detected: User [ID: {}] is a GUARDIAN. Guardian ID: {}", userId, guardian.getId());
            response.setGuardianDetails(guardianMapper.toDto(guardian));
        });

        log.info("Successfully assembled comprehensive profile for User ID: {}", userId);
        return response;
    }

    /**
     * Updates the basic personal information for a user.
     * <p>
     * Note: This method currently only updates the `UserProfile` entity (Bio, Names, DOB).
     * It does not update Role-specific data (e.g., Staff Job Title) or Account data (e.g., Email/Password).
     * </p>
     *
     * @param userId    The ID of the user performing the update.
     * @param updateDto The subset of fields allowed to be updated.
     * @return The updated profile data.
     * @throws ResourceNotFoundException if the user or profile is missing.
     */
    @Override
    @Transactional
    public UserProfileDTO updateProfileByUserId(Long userId, UserProfileUpdateDTO updateDto) {
        log.info("Request received to update profile for User ID: {}", userId);

        // 1. Verify User existence
        User user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> {
                    log.error("Update failed: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });

        // 2. Fetch existing Profile
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Update failed: UserProfile not found for User ID: {}", userId);
                    return new ResourceNotFoundException("UserProfile", "userId", userId);
                });

        // 3. Perform Partial Update
        log.debug("Applying changes to UserProfile ID: {}", profile.getId());
        userProfileMapper.updateEntityFromDto(updateDto, profile);

        // 4. Save and Return
        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("Profile updated successfully for User ID: {}", userId);

        return userProfileMapper.toDto(savedProfile, user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileImageUploadInitResponseDTO initiateProfileImageUpload(Long userId, ProfileImageUploadInitRequestDTO request) {
        log.info("Request received to initialize profile image upload for User ID: {}", userId);

        validateUploadInitRequest(request);
        User user = findUserById(userId);
        String objectKey = buildObjectKey(user, request.getFileName());
        Instant expiresAt = Instant.now().plusSeconds(mediaUploadProperties.getUploadInitTtlSeconds());

        String provider = normalizeProvider(mediaUploadProperties.getProvider());
        if ("cloudinary".equals(provider)) {
            MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
            if (!StringUtils.hasText(cfg.getCloudName()) || !StringUtils.hasText(cfg.getApiKey()) || !StringUtils.hasText(cfg.getApiSecret())) {
                throw new EdusyncException("Cloudinary upload is not configured properly.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String folder = cfg.getFolder();
            String signatureBase = "folder=" + folder + "&public_id=" + objectKey + "&timestamp=" + timestamp;
            String signature = sha1Hex(signatureBase + cfg.getApiSecret());

            Map<String, String> fields = new HashMap<>();
            fields.put("api_key", cfg.getApiKey());
            fields.put("timestamp", timestamp);
            fields.put("signature", signature);
            fields.put("folder", folder);
            fields.put("public_id", objectKey);

            return ProfileImageUploadInitResponseDTO.builder()
                    .provider("cloudinary")
                    .method("POST")
                    .uploadUrl("https://api.cloudinary.com/v1_1/" + cfg.getCloudName() + "/image/upload")
                    .objectKey(objectKey)
                    .expiresAt(expiresAt)
                    .fields(fields)
                    .headers(Map.of())
                    .build();
        }

        if ("s3".equals(provider)) {
            String template = mediaUploadProperties.getS3().getUploadUrlTemplate();
            if (!StringUtils.hasText(template)) {
                throw new EdusyncException("S3 upload URL template is not configured.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            String uploadUrl = template.replace("{objectKey}", objectKey);
            return ProfileImageUploadInitResponseDTO.builder()
                    .provider("s3")
                    .method("PUT")
                    .uploadUrl(uploadUrl)
                    .objectKey(objectKey)
                    .expiresAt(expiresAt)
                    .fields(Map.of())
                    .headers(Map.of("Content-Type", request.getContentType()))
                    .build();
        }

        throw new EdusyncException("Unsupported media provider: " + provider, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    @Transactional
    public UserProfileDTO completeProfileImageUpload(Long userId, ProfileImageUploadCompleteRequestDTO request) {
        log.info("Request received to complete profile image upload for User ID: {}", userId);

        User user = findUserById(userId);
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "userId", userId));

        String expectedPrefix = "profiles/" + user.getUuid() + "/";
        if (!request.getObjectKey().startsWith(expectedPrefix)) {
            throw new EdusyncException("Invalid objectKey for this user.", HttpStatus.FORBIDDEN);
        }

        validateSecureUrl(request.getSecureUrl());
        profile.setProfileUrl(request.getSecureUrl());

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Profile image URL saved successfully for User ID: {}", userId);
        return userProfileMapper.toDto(saved, user);
    }

    private void validateUploadInitRequest(ProfileImageUploadInitRequestDTO request) {
        if (request.getSizeBytes() > mediaUploadProperties.getMaxFileSizeBytes()) {
            throw new EdusyncException("File size exceeds allowed limit.", HttpStatus.BAD_REQUEST);
        }
        if (mediaUploadProperties.getAllowedContentTypes().stream().noneMatch(t -> t.equalsIgnoreCase(request.getContentType()))) {
            throw new EdusyncException("Unsupported content type: " + request.getContentType(), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateSecureUrl(String secureUrl) {
        if (!StringUtils.hasText(secureUrl) || !secureUrl.startsWith("https://")) {
            throw new EdusyncException("secureUrl must be HTTPS.", HttpStatus.BAD_REQUEST);
        }
        List<String> prefixes = mediaUploadProperties.getAllowedSecureUrlPrefixes();
        if (prefixes != null && !prefixes.isEmpty()) {
            boolean allowed = prefixes.stream().anyMatch(secureUrl::startsWith);
            if (!allowed) {
                throw new EdusyncException("secureUrl is not from an allowed CDN host.", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private String buildObjectKey(User user, String fileName) {
        String safeName = sanitizeFileName(fileName);
        return "profiles/" + user.getUuid() + "/" + Instant.now().getEpochSecond() + "_" + UUID.randomUUID() + "_" + safeName;
    }

    private String sanitizeFileName(String fileName) {
        String base = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return StringUtils.hasText(base) ? base : "avatar.jpg";
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "cloudinary" : provider.trim().toLowerCase();
    }

    private String sha1Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new EdusyncException("Could not generate upload signature.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}