package com.ezproject.poker;

import com.ezproject.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "poker_vote")
public class PokerVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id")
    private PokerRound round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voter_id")
    private AppUser voter;

    @Column(nullable = false)
    private String card;

    protected PokerVote() {
        // JPA
    }

    PokerVote(PokerRound round, AppUser voter, String card) {
        this.round = round;
        this.voter = voter;
        this.card = card;
    }

    void changeCard(String newCard) {
        this.card = newCard;
    }

    public Long getId() { return id; }
    public PokerRound getRound() { return round; }
    public AppUser getVoter() { return voter; }
    public String getCard() { return card; }
}
