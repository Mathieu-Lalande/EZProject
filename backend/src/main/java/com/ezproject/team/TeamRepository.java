package com.ezproject.team;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByOrganizationIdOrderByNameAsc(Long organizationId);

    Optional<Team> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Team> findByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);
}
