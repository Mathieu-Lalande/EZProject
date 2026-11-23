package com.ezproject.poker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ezproject.common.BusinessRuleException;
import com.ezproject.organization.Organization;
import com.ezproject.organization.Plan;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectType;
import com.ezproject.user.AppUser;
import com.ezproject.user.Role;

class PokerRoundTest {

    private PokerRound round;
    private AppUser alice;
    private AppUser bob;

    @BeforeEach
    void setUp() {
        Organization org = new Organization("Org", "org", Plan.FREE);
        round = new PokerRound(new Project(org, null, "DEMO", "Démo", ProjectType.DEVELOPMENT), null, "Story");
        alice = user(1L, "Alice");
        bob = user(2L, "Bob");
    }

    @Test
    void aVoterCanChangeTheirCardWhileVoting() {
        round.vote(alice, "3");
        round.vote(alice, "5");

        assertThat(round.getVotes()).singleElement().extracting(PokerVote::getCard).isEqualTo("5");
    }

    @Test
    void votesAreRefusedOnceRevealed() {
        round.vote(alice, "3");
        round.reveal();

        assertThatThrownBy(() -> round.vote(bob, "8")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unknownCardIsRejected() {
        assertThatThrownBy(() -> round.vote(alice, "4")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void statsSuggestNextCardAboveAverage() {
        round.vote(alice, "3");
        round.vote(bob, "8");

        RoundDto.Stats stats = PokerService.stats(round.getVotes());

        assertThat(stats.average()).isEqualTo(5.5);
        assertThat(stats.suggested()).isEqualTo("8");
        assertThat(stats.consensus()).isFalse();
    }

    @Test
    void restartClearsVotes() {
        round.vote(alice, "3");
        round.reveal();
        round.restart();

        assertThat(round.getVotes()).isEmpty();
        assertThat(round.getStatus()).isEqualTo(RoundStatus.VOTING);
    }

    private static AppUser user(Long id, String name) {
        AppUser u = new AppUser(new Organization("Org", "org", Plan.FREE), name + "@test.local", name, "x", Role.MEMBER, null);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}
