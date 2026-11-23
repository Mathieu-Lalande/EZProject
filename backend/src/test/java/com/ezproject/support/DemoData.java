package com.ezproject.support;

import org.springframework.stereotype.Component;

import com.ezproject.project.ProjectRepository;
import com.ezproject.team.TeamRepository;
import com.ezproject.user.AppUserRepository;

/** Accès aux identifiants du jeu de démo (profil dev) depuis les tests d'intégration. */
@Component
public class DemoData {

    private final ProjectRepository projects;
    private final AppUserRepository users;
    private final TeamRepository teams;

    public DemoData(ProjectRepository projects, AppUserRepository users, TeamRepository teams) {
        this.projects = projects;
        this.users = users;
        this.teams = teams;
    }

    public long project(String code) {
        return projects.findAll().stream().filter(p -> p.getCode().equals(code)).findFirst().orElseThrow().getId();
    }

    public long user(String login) {
        return users.findAll().stream().filter(u -> u.getEmail().startsWith(login + "@")).findFirst().orElseThrow().getId();
    }

    public long team(String name) {
        return teams.findAll().stream().filter(t -> t.getName().equals(name)).findFirst().orElseThrow().getId();
    }
}
