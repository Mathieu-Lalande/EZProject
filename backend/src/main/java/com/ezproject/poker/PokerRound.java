package com.ezproject.poker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ezproject.common.BusinessRuleException;
import com.ezproject.project.Project;
import com.ezproject.task.Task;
import com.ezproject.user.AppUser;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** Un tour de planning poker sur une story. Agrégat : les votes ne se modifient qu'à travers lui. */
@Entity
@Table(name = "poker_round")
public class PokerRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "story_title", nullable = false)
    private String storyTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status;

    @Column(name = "final_estimate")
    private String finalEstimate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PokerVote> votes = new ArrayList<>();

    protected PokerRound() {
        // JPA
    }

    public PokerRound(Project project, Task task, String storyTitle) {
        this.project = project;
        this.task = task;
        this.storyTitle = storyTitle;
        this.status = RoundStatus.VOTING;
        this.createdAt = Instant.now();
    }

    public void vote(AppUser voter, String card) {
        if (status != RoundStatus.VOTING) {
            throw new BusinessRuleException("Les votes sont clos pour ce tour");
        }
        if (!PokerDeck.isValid(card)) {
            throw new BusinessRuleException("Carte inconnue : " + card);
        }
        votes.stream()
                .filter(v -> v.getVoter().getId().equals(voter.getId()))
                .findFirst()
                .ifPresentOrElse(v -> v.changeCard(card), () -> votes.add(new PokerVote(this, voter, card)));
    }

    public void reveal() {
        if (votes.isEmpty()) {
            throw new BusinessRuleException("Aucun vote à révéler");
        }
        this.status = RoundStatus.REVEALED;
    }

    /** Relance un vote : efface les cartes et repasse en phase de vote. */
    public void restart() {
        votes.clear();
        this.finalEstimate = null;
        this.status = RoundStatus.VOTING;
    }

    public void close(String estimate) {
        if (!PokerDeck.isValid(estimate)) {
            throw new BusinessRuleException("Estimation invalide : " + estimate);
        }
        this.finalEstimate = estimate;
        this.status = RoundStatus.CLOSED;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public Task getTask() { return task; }
    public String getStoryTitle() { return storyTitle; }
    public RoundStatus getStatus() { return status; }
    public String getFinalEstimate() { return finalEstimate; }
    public Instant getCreatedAt() { return createdAt; }
    public List<PokerVote> getVotes() { return List.copyOf(votes); }
}
