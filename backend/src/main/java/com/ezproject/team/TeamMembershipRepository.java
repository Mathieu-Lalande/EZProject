package com.ezproject.team;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {

    @EntityGraph(attributePaths = "team")
    List<TeamMembership> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<TeamMembership> findByTeamIdIn(List<Long> teamIds);

    Optional<TeamMembership> findByTeamIdAndUserId(Long teamId, Long userId);
}
