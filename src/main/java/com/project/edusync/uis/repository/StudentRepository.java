package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEnrollmentNumber(String enrollmentNumber);

    Optional<Student> findByUserProfile(UserProfile profile);

    /**
     * Fetches all students with their UserProfile, User, Section and AcademicClass eagerly.
     * Explicit countQuery is required when using JOIN FETCH with pagination.
     */
    @Query(value = "SELECT s FROM Student s " +
                   "JOIN FETCH s.userProfile up " +
                   "JOIN FETCH up.user u " +
                   "JOIN FETCH s.section sec " +
                   "JOIN FETCH sec.academicClass ac " +
                   "WHERE (:active IS NULL OR u.isActive = :active)",
           countQuery = "SELECT COUNT(s) FROM Student s " +
                        "JOIN s.userProfile up " +
                        "JOIN up.user u " +
                        "WHERE (:active IS NULL OR u.isActive = :active)")
    Page<Student> findAllWithDetails(@Param("active") Boolean active, Pageable pageable);

    /**
     * Search students by name, email, or enrollment number (case-insensitive).
     * Explicit countQuery required for JOIN FETCH + pagination.
     */
    @Query(value = "SELECT s FROM Student s " +
                   "JOIN FETCH s.userProfile up " +
                   "JOIN FETCH up.user u " +
                   "JOIN FETCH s.section sec " +
                   "JOIN FETCH sec.academicClass ac " +
                   "WHERE (:active IS NULL OR u.isActive = :active) AND (" +
                   "LOWER(up.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(up.lastName)     LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(u.email)         LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(s.enrollmentNumber) LIKE LOWER(CONCAT('%', :query, '%'))) ",
           countQuery = "SELECT COUNT(s) FROM Student s " +
                        "JOIN s.userProfile up " +
                        "JOIN up.user u " +
                        "WHERE (:active IS NULL OR u.isActive = :active) AND (" +
                        "LOWER(up.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(up.lastName)     LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(u.email)         LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(s.enrollmentNumber) LIKE LOWER(CONCAT('%', :query, '%'))) ")
    Page<Student> searchStudents(@Param("query") String query, @Param("active") Boolean active, Pageable pageable);

    Optional<Student> findByUuid(java.util.UUID uuid);
}
