package com.ezproject.project;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.user.AppUser;
import com.ezproject.user.UserService;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private static final String DEFAULT_COLOR = "#6366f1";

    private final ProjectRepository repository;
    private final UserService userService;

    public ProjectService(ProjectRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public List<ProjectDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(ProjectDto::from).toList();
    }

    public ProjectDto get(Long id) {
        return ProjectDto.from(getEntity(id));
    }

    public Project getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Projet", id));
    }

    /** Vérifie qu'un module est activé avant d'autoriser son usage sur le projet. */
    public Project requireModule(Long projectId, ModuleType module) {
        Project project = getEntity(projectId);
        if (!project.hasModule(module)) {
            throw new BusinessRuleException("Le module " + module + " n'est pas activé sur ce projet");
        }
        return project;
    }

    @Transactional
    public ProjectDto create(ProjectRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) {
            throw new BusinessRuleException("Le code projet " + request.code() + " est déjà utilisé");
        }
        Project project = new Project(request.code(), request.name(), request.type());
        apply(project, request);
        return ProjectDto.from(repository.save(project));
    }

    @Transactional
    public ProjectDto update(Long id, ProjectRequest request) {
        Project project = getEntity(id);
        if (!project.getCode().equalsIgnoreCase(request.code())) {
            throw new BusinessRuleException("Le code projet ne peut pas être modifié");
        }
        apply(project, request);
        return ProjectDto.from(project);
    }

    @Transactional
    public ProjectDto updateModules(Long id, ModulesRequest request) {
        Project project = getEntity(id);
        project.replaceModules(request.modules());
        return ProjectDto.from(project);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private void apply(Project project, ProjectRequest r) {
        AppUser owner = r.ownerId() == null ? null : userService.getEntity(r.ownerId());
        project.update(r.name(), r.description(), r.client(), r.type(),
                Objects.requireNonNullElse(r.status(), project.getStatus()),
                Objects.requireNonNullElse(r.health(), project.getHealth()),
                Objects.requireNonNullElse(r.color(), DEFAULT_COLOR),
                owner, r.startDate(), r.endDate(), r.budgetDays());
    }
}
