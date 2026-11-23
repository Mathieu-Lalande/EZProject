package com.ezproject.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = "organization")
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findByOrganizationIdOrderByDisplayNameAsc(Long organizationId);

    Optional<AppUser> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<AppUser> findByEmailIgnoreCaseAndOrganizationId(String email, Long organizationId);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndRole(Long organizationId, Role role);
}
