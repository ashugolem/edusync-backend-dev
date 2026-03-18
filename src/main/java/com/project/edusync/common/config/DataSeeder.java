package com.project.edusync.common.config;

import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.AcademicClassRepository;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment; // Import the Environment class
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This component runs once on application startup.
 * It checks if the spring.jpa.hibernate.ddl-auto property is set to 'create'.
 * If it is, it populates the database with foundational data (Roles, Classes).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final AcademicClassRepository classRepository;
    private final Environment environment; // 1. Inject the Spring Environment

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        // 2. Get the ddl-auto property value from the environment
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");

        // 3. Check if the mode is 'create'
        if ("create".equalsIgnoreCase(ddlAuto)) {
            log.info("DDL-AUTO mode is 'create'. Running initial data seeder...");

            // Run the seeding logic
            seedRoles();
            seedClassesAndSections();

            log.info("Data seeding complete.");
        } else {
            // 4. Log and skip if not in 'create' mode
            log.info("DDL-AUTO mode is not 'create' (value: {}). Skipping data seeder.", ddlAuto);
        }
    }

    private void seedRoles() {
        // No .count() check needed, as 'create' mode guarantees empty tables.
        log.info("Seeding foundational Roles...");

        List<Role> roles = Arrays.asList(
                createRole("ROLE_STUDENT"),
                createRole("ROLE_TEACHER"),
                createRole("ROLE_PRINCIPAL"),
                createRole("ROLE_LIBRARIAN"),
                createRole("ROLE_GUARDIAN"),
                createRole("ROLE_ADMIN"),
                createRole("ROLE_SUPER_ADMIN"),// For future use, not assigned to anyone yet
                createRole("ROLE_SCHOOL_ADMIN") // For future use, not assigned to anyone yet
        );

        roleRepository.saveAll(roles);
        log.info("Saved {} roles.", roles.size());
    }

    private Role createRole(String name) {
        Role role = new Role();
        role.setName(name);
        role.setActive(true);
        // Note: uuid, createdAt, etc., are set automatically
        // by AuditableEntity's @PrePersist and @CreatedDate
        return role;
    }

    private void seedClassesAndSections() {
        // No .count() check needed.
        log.info("Seeding foundational Academic Classes and Sections...");

        // Use the helper to create classes and their sections in one go
        createClassWithSections("Nursery", Arrays.asList("A", "B"));
        createClassWithSections("LKG", Arrays.asList("A", "B"));
        createClassWithSections("UKG", Arrays.asList("A", "B"));

        List<String> standardSections = Arrays.asList("A", "B", "C");
        for (int i = 1; i <= 12; i++) {
            createClassWithSections("Class " + i, standardSections);
        }

        log.info("Saved {} classes and their corresponding sections.", classRepository.count());
    }

    /**
     * Helper method to create an AcademicClass and its child Sections.
     * This leverages CascadeType.ALL on the 'sections' relationship
     * in the AcademicClass entity.
     */
    private void createClassWithSections(String className, List<String> sectionNames) {
        // 1. Create the parent (AcademicClass)
        AcademicClass ac = new AcademicClass();
        ac.setName(className);
        ac.setIsActive(true);

        // 2. Create the children (Sections) and link them to the parent
        Set<Section> sections = new HashSet<>();
        for (String sectionName : sectionNames) {
            Section section = new Section();
            section.setSectionName(sectionName);
            section.setAcademicClass(ac); // Link child to parent
            sections.add(section);
        }

        // 3. Set the relationship on the parent side
        ac.setSections(sections);

        // 4. Save the parent.
        // Because of @OneToMany(mappedBy = "academicClass", cascade = CascadeType.ALL...)
        // saving the parent (AcademicClass) will automatically save all the
        // linked Section entities in the same transaction.
        classRepository.save(ac);
        log.debug("Created class: {} with sections: {}", className, sectionNames);
    }
}