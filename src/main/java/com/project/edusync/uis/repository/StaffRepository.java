package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.enums.StaffType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    boolean existsByEmployeeId(String employeeId);

    Optional<Staff> findByUserProfile(UserProfile userProfile);

    /**
     * Fetches all staff with their UserProfile and User eagerly.
     * Explicit countQuery is required when using JOIN FETCH with pagination.
     */
    @Query(value = "SELECT st FROM Staff st " +
                   "JOIN FETCH st.userProfile up " +
                   "JOIN FETCH up.user u " +
                   "WHERE (:active IS NULL OR u.isActive = :active)",
           countQuery = "SELECT COUNT(st) FROM Staff st " +
                        "JOIN st.userProfile up " +
                        "JOIN up.user u " +
                        "WHERE (:active IS NULL OR u.isActive = :active)")
    Page<Staff> findAllWithDetails(@Param("active") Boolean active, Pageable pageable);

    /**
     * Fetches staff filtered by StaffType with eager joins.
     */
    @Query(value = "SELECT st FROM Staff st " +
                   "JOIN FETCH st.userProfile up " +
                   "JOIN FETCH up.user u " +
                   "WHERE st.staffType = :staffType AND (:active IS NULL OR u.isActive = :active)",
           countQuery = "SELECT COUNT(st) FROM Staff st " +
                        "JOIN st.userProfile up " +
                        "JOIN up.user u " +
                        "WHERE st.staffType = :staffType AND (:active IS NULL OR u.isActive = :active)")
    Page<Staff> findAllByStaffTypeWithDetails(@Param("staffType") StaffType staffType, @Param("active") Boolean active, Pageable pageable);

    /**
     * Search staff by name, email, employeeId, or jobTitle (case-insensitive).
     */
    @Query(value = "SELECT st FROM Staff st " +
                   "JOIN FETCH st.userProfile up " +
                   "JOIN FETCH up.user u " +
                   "WHERE (:active IS NULL OR u.isActive = :active) AND (" +
                   "LOWER(up.firstName)  LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(up.lastName)      LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(u.email)          LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(st.employeeId)    LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(st.jobTitle)      LIKE LOWER(CONCAT('%', :query, '%'))) ",
           countQuery = "SELECT COUNT(st) FROM Staff st " +
                        "JOIN st.userProfile up " +
                        "JOIN up.user u " +
                        "WHERE (:active IS NULL OR u.isActive = :active) AND (" +
                        "LOWER(up.firstName)  LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(up.lastName)      LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(u.email)          LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(st.employeeId)    LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(st.jobTitle)      LIKE LOWER(CONCAT('%', :query, '%'))) ")
    Page<Staff> searchStaff(@Param("query") String query, @Param("active") Boolean active, Pageable pageable);

    Optional<Staff> findByUuid(java.util.UUID uuid);
}
