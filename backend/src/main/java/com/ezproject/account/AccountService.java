package com.ezproject.account;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.audit.AuditService;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.security.CurrentUser;
import com.ezproject.task.TaskRepository;
import com.ezproject.task.TaskStatus;
import com.ezproject.team.TeamMembershipRepository;
import com.ezproject.todo.TodoRepository;
import com.ezproject.user.AppUser;
import com.ezproject.user.AppUserRepository;
import com.ezproject.user.Role;
import com.ezproject.user.UserDto;

/** Espace personnel : profil, mot de passe, portabilité et effacement des données (RGPD). */
@Service
@Transactional(readOnly = true)
public class AccountService {

    private final CurrentUser currentUser;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final TeamMembershipRepository memberships;
    private final TodoRepository todos;
    private final TaskRepository tasks;
    private final List<PersonalDataContributor> contributors;

    public AccountService(CurrentUser currentUser, AppUserRepository users, PasswordEncoder encoder,
                          AuditService audit, TeamMembershipRepository memberships, TodoRepository todos,
                          TaskRepository tasks, List<PersonalDataContributor> contributors) {
        this.currentUser = currentUser;
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
        this.memberships = memberships;
        this.todos = todos;
        this.tasks = tasks;
        this.contributors = contributors;
    }

    @Transactional
    public UserDto updateProfile(String displayName, String jobTitle) {
        AppUser me = currentUser.get();
        me.updateProfile(displayName.trim(), jobTitle == null || jobTitle.isBlank() ? null : jobTitle.trim());
        audit.record("PROFILE_UPDATED", "USER", me.getId(), "Profil mis à jour");
        return UserDto.from(me);
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        AppUser me = currentUser.get();
        if (!encoder.matches(currentPassword, me.getPasswordHash())) {
            throw new BusinessRuleException("Mot de passe actuel incorrect");
        }
        if (currentPassword.equals(newPassword)) {
            throw new BusinessRuleException("Le nouveau mot de passe doit être différent de l'actuel");
        }
        PasswordPolicy.check(newPassword, me.getEmail());
        me.changePassword(encoder.encode(newPassword));
        audit.record("PASSWORD_CHANGED", "USER", me.getId(), "Mot de passe modifié");
    }

    /** Droit à la portabilité : toutes les données personnelles de l'utilisateur, en JSON. */
    @Transactional // pas en lecture seule : l'export est tracé dans le journal d'audit
    public Map<String, Object> exportMyData() {
        AppUser me = currentUser.get();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", Instant.now());
        data.put("profile", Map.of("email", me.getEmail(), "displayName", me.getDisplayName(),
                "jobTitle", me.getJobTitle() == null ? "" : me.getJobTitle(), "role", me.getRole(),
                "organization", me.getOrganization().getName(), "createdAt", me.getCreatedAt()));
        data.put("teams", memberships.findByUserId(me.getId()).stream()
                .map(m -> Map.of("team", m.getTeam().getName(), "role", m.getRole())).toList());
        data.put("personalTodos", todos.findByOwnerIdAndProjectIsNullOrderByDoneAscCreatedAtDesc(me.getId()).stream()
                .map(t -> Map.of("title", t.getTitle(), "done", t.isDone(),
                        "dueDate", t.getDueDate() == null ? "" : t.getDueDate().toString())).toList());
        data.put("assignedOpenTasks", tasks.findByAssigneeIdAndStatusNotOrderByDueDateAsc(me.getId(), TaskStatus.DONE)
                .stream().map(t -> Map.of("project", t.getProject().getCode(), "title", t.getTitle(),
                        "status", t.getStatus())).toList());
        contributors.forEach(c -> data.put(c.key(), c.export(me)));
        audit.record("PERSONAL_DATA_EXPORTED", "USER", me.getId(), "Export des données personnelles");
        return data;
    }

    /** Droit à l'effacement : le compte est anonymisé (l'historique projet reste cohérent). */
    @Transactional
    public void deleteMyAccount(String password) {
        AppUser me = currentUser.get();
        if (!encoder.matches(password, me.getPasswordHash())) {
            throw new BusinessRuleException("Mot de passe incorrect");
        }
        if (me.getRole() == Role.OWNER
                && users.findByOrganizationIdAndDeletedAtIsNullOrderByDisplayNameAsc(me.getOrganization().getId())
                .stream().noneMatch(u -> u.getRole() == Role.OWNER && !u.getId().equals(me.getId()) && u.isActive())) {
            throw new BusinessRuleException("Vous êtes le seul propriétaire : nommez un autre propriétaire "
                    + "ou supprimez l'organisation");
        }
        audit.record("ACCOUNT_DELETED", "USER", me.getId(), "Suppression de son compte par " + me.getDisplayName());
        anonymize(me);
    }

    /** Anonymisation commune (utilisateur lui-même ou administrateur). */
    @Transactional
    public void anonymize(AppUser user) {
        memberships.findByUserId(user.getId()).forEach(memberships::delete);
        contributors.forEach(c -> c.erase(user));
        user.anonymize(encoder.encode(PasswordPolicy.temporary() + PasswordPolicy.temporary()));
    }
}
