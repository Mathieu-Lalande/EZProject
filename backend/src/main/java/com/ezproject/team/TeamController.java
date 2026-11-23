package com.ezproject.team;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) {
        this.service = service;
    }

    @GetMapping
    public List<TeamDto> list() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto create(@Valid @RequestBody TeamRequest request) {
        return service.create(request);
    }

    @PutMapping("/{teamId}")
    public TeamDto update(@PathVariable Long teamId, @Valid @RequestBody TeamRequest request) {
        return service.update(teamId, request);
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long teamId) {
        service.delete(teamId);
    }

    @PutMapping("/{teamId}/members/{userId}")
    public TeamDto putMember(@PathVariable Long teamId, @PathVariable Long userId,
                             @Valid @RequestBody TeamMemberRequest request) {
        return service.putMember(teamId, userId, request.role());
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public TeamDto removeMember(@PathVariable Long teamId, @PathVariable Long userId) {
        return service.removeMember(teamId, userId);
    }
}
