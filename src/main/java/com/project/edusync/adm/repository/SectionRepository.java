package com.project.edusync.adm.repository;

import com.project.edusync.adm.model.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, Long> {
    @Query("SELECT s FROM Section s JOIN s.academicClass ac WHERE ac.name = :className AND s.sectionName = :sectionName")
    Optional<Section> findByAcademicClass_NameAndSectionName(String className, String sectionName);

    @Query("SELECT s FROM Section s JOIN FETCH s.academicClass")
    List<Section> findAllWithClass();

    @Query("SELECT s FROM Section s where s.uuid = :sectionId")
    Optional<Section> findById(UUID sectionId);

    @Query("SELECT s from Section s Where s.uuid = :sectionId")
    boolean existsById(UUID sectionId);

    @Query("UPDATE Section s SET s.isActive = false WHERE s.uuid = :sectionId")
    void softDeleteById(UUID sectionId);

    Optional<Section> findByUuid(UUID uuid);
}
