package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.StudentGuardianRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentGuardianRelationshipRepository extends JpaRepository<StudentGuardianRelationship, Long> {
    Optional<StudentGuardianRelationship> findByStudentAndGuardian(Student student, Guardian guardian);
    boolean existsByStudentAndGuardian(Student student, Guardian guardian);
    List<StudentGuardianRelationship> findByStudent(Student student);
    List<StudentGuardianRelationship> findByGuardian(Guardian guardian);
    void deleteByStudentAndGuardian(Student student, Guardian guardian);
}

