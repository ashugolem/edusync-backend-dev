package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Optional<Guardian> findByUserProfile(UserProfile userProfile);
    Optional<Guardian> findByUuid(UUID uuid);
}
