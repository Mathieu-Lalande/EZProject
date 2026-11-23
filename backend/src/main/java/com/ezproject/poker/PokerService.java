package com.ezproject.poker;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectService;
import com.ezproject.security.CurrentUser;
import com.ezproject.task.Task;
import com.ezproject.task.TaskService;
import com.ezproject.user.AppUser;

@Service
@Transactional(readOnly = true)
public class PokerService {

    private final PokerRoundRepository repository;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final CurrentUser currentUser;

    public PokerService(PokerRoundRepository repository, ProjectService projectService, TaskService taskService,
                        CurrentUser currentUser) {
        this.repository = repository;
        this.projectService = projectService;
        this.taskService = taskService;
        this.currentUser = currentUser;
    }

    public List<RoundDto> findByProject(Long projectId) {
        projectService.getEntity(projectId);
        AppUser me = currentUser.get();
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(r -> toDto(r, me)).toList();
    }

    public RoundDto get(Long roundId) {
        return toDto(getEntity(roundId), currentUser.get());
    }

    @Transactional
    public RoundDto start(Long projectId, NewRoundRequest request) {
        Project project = projectService.requireModule(projectId, ModuleType.POKER);
        Task task = null;
        String title = request.storyTitle();
        if (request.taskId() != null) {
            task = taskService.getEntity(request.taskId());
            if (!task.getProject().getId().equals(projectId)) {
                throw new BusinessRuleException("La tâche n'appartient pas à ce projet");
            }
            title = task.getTitle();
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessRuleException("Indiquez une story ou une tâche à estimer");
        }
        return toDto(repository.save(new PokerRound(project, task, title)), currentUser.get());
    }

    @Transactional
    public RoundDto vote(Long roundId, VoteRequest request) {
        PokerRound round = getEntity(roundId);
        AppUser me = currentUser.get();
        round.vote(me, request.card());
        return toDto(round, me);
    }

    @Transactional
    public RoundDto reveal(Long roundId) {
        PokerRound round = getEntity(roundId);
        round.reveal();
        return toDto(round, currentUser.get());
    }

    @Transactional
    public RoundDto restart(Long roundId) {
        PokerRound round = getEntity(roundId);
        round.restart();
        repository.flush();
        return toDto(round, currentUser.get());
    }

    /** Clôt le tour et reporte l'estimation sur la tâche liée, le cas échéant. */
    @Transactional
    public RoundDto close(Long roundId, VoteRequest request) {
        PokerRound round = getEntity(roundId);
        round.close(request.card());
        OptionalInt points = PokerDeck.numericValue(request.card());
        if (round.getTask() != null && points.isPresent()) {
            taskService.estimate(round.getTask().getId(), points.getAsInt());
        }
        return toDto(round, currentUser.get());
    }

    private PokerRound getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Tour de poker", id));
    }

    RoundDto toDto(PokerRound round, AppUser me) {
        boolean visible = round.getStatus() != RoundStatus.VOTING;
        List<RoundDto.VoteView> votes = round.getVotes().stream()
                .sorted(Comparator.comparing(v -> v.getVoter().getDisplayName()))
                .map(v -> new RoundDto.VoteView(v.getVoter().getId(), v.getVoter().getDisplayName(),
                        visible ? v.getCard() : null))
                .toList();
        String myCard = round.getVotes().stream()
                .filter(v -> v.getVoter().getId().equals(me.getId()))
                .map(PokerVote::getCard).findFirst().orElse(null);
        return new RoundDto(round.getId(), round.getTask() == null ? null : round.getTask().getId(),
                round.getStoryTitle(), round.getStatus(), round.getFinalEstimate(), myCard, votes,
                visible ? stats(round.getVotes()) : null);
    }

    /** Moyenne des cartes numériques et carte du jeu la plus proche au-dessus (arrondi prudent). */
    static RoundDto.Stats stats(List<PokerVote> votes) {
        List<Integer> numeric = votes.stream()
                .map(v -> PokerDeck.numericValue(v.getCard()))
                .filter(OptionalInt::isPresent).map(OptionalInt::getAsInt).toList();
        if (numeric.isEmpty()) {
            return new RoundDto.Stats(null, null, false);
        }
        double avg = numeric.stream().mapToInt(Integer::intValue).average().orElse(0);
        String suggested = PokerDeck.CARDS.stream()
                .filter(c -> PokerDeck.numericValue(c).isPresent())
                .filter(c -> PokerDeck.numericValue(c).getAsInt() >= avg)
                .findFirst().orElse("40");
        boolean consensus = votes.stream().map(PokerVote::getCard).distinct().count() == 1
                && votes.stream().allMatch(v -> Objects.nonNull(v.getCard()));
        return new RoundDto.Stats(Math.round(avg * 10) / 10.0, suggested, consensus);
    }
}
