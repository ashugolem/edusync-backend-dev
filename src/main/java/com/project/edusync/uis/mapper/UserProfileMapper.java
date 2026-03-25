package com.project.edusync.uis.mapper;

import com.project.edusync.iam.model.dto.CreateUserRequestDTO;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.model.dto.profile.UserProfileDTO;
import com.project.edusync.uis.model.dto.profile.UserProfileResponseDTO;
import com.project.edusync.uis.model.dto.profile.UserProfileUpdateDTO;
import com.project.edusync.uis.model.entity.UserProfile;
import org.mapstruct.*;

/**
 * Primary Mapper for the UserProfile entity.
 * <p>
 * This mapper is responsible for merging the "Identity" (User table)
 * with the "Person" (UserProfile table) to create a complete view.
 * </p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfileMapper {
    // =========================================================================
    // Create Mappings (DTO -> Entity)
    // =========================================================================

    /**
     * Converts the registration request into a UserProfile entity.
     * We ignore 'id' (DB generated) and 'user' (linked manually in Service).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    // Fields like firstName, lastName, bio, dateOfBirth map automatically by name
    UserProfile toEntity(CreateUserRequestDTO dto);

    // --- Response Mappings ---

    /**
     * Merges User and UserProfile into the comprehensive Response DTO.
     *
     * @param profile The person's profile data (Names, Bio, DOB).
     * @param user    The person's login data (Username, Email).
     * @return A consolidated DTO.
     */
    @Mapping(source = "profile.id", target = "id")
    @Mapping(source = "profile.uuid", target = "uuid")
    @Mapping(source = "profile.profileUrl", target = "profileUrl")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "profile.createdAt", target = "createdAt")
    @Mapping(source = "profile.updatedAt", target = "updatedAt")
    // Fields like 'firstName', 'lastName', 'bio' map automatically
    UserProfileResponseDTO toResponseDto(UserProfile profile, User user);

    /**
     * Simplified DTO mapping often used for basic return values after updates.
     */
    @Mapping(source = "profile.id", target = "profileId")
    @Mapping(source = "profile.profileUrl", target = "profileUrl")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    UserProfileDTO toDto(UserProfile profile, User user);

    // --- Update Mappings ---

    /**
     * Applies updates from a DTO to an existing Entity.
     * <p>
     * <b>Strategy:</b> nullValuePropertyMappingStrategy = IGNORE.
     * This is critical. If the DTO passes 'null' for a field (e.g., user didn't change their Bio),
     * we do NOT want to overwrite the existing database value with null. We only update
     * fields that are actually present.
     * </p>
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UserProfileUpdateDTO dto, @MappingTarget UserProfile entity);
}