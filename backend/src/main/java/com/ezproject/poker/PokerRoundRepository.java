package com.ezproject.poker;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PokerRoundRepository extends JpaRepository<PokerRound, Long> {

    List<PokerRound> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
