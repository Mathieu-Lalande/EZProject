package com.ezproject.user;

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

import com.ezproject.account.UserAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserAdminService adminService;

    public UserController(UserService userService, UserAdminService adminService) {
        this.userService = userService;
        this.adminService = adminService;
    }

    public record ActiveRequest(boolean active) {
    }

    @GetMapping
    public List<UserDto> list() {
        return userService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}/role")
    public UserDto changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request) {
        return userService.changeRole(id, request.role());
    }

    @PostMapping("/{id}/reset-password")
    public UserAdminService.TemporaryPassword resetPassword(@PathVariable Long id) {
        return adminService.resetPassword(id);
    }

    @PutMapping("/{id}/active")
    public UserDto setActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return adminService.setActive(id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        adminService.delete(id);
    }
}
