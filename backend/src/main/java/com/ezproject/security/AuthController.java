package com.ezproject.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ezproject.user.UserDto;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUser currentUser;

    public AuthController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public UserDto me() {
        return UserDto.from(currentUser.get());
    }
}
