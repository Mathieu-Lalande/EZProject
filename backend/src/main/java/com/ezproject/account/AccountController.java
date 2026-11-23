package com.ezproject.account;

import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ezproject.user.UserDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    public record ProfileRequest(@NotBlank @Size(max = 120) String displayName, @Size(max = 120) String jobTitle) {
    }

    public record PasswordRequest(@NotBlank @Size(max = 128) String currentPassword,
                                  @NotBlank @Size(min = 12, max = 128, message = "12 caractères minimum") String newPassword) {
    }

    public record DeleteRequest(@NotBlank @Size(max = 128) String password) {
    }

    @PutMapping("/profile")
    public UserDto updateProfile(@Valid @RequestBody ProfileRequest request) {
        return service.updateProfile(request.displayName(), request.jobTitle());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody PasswordRequest request, HttpServletRequest http) {
        service.changePassword(request.currentPassword(), request.newPassword());
        // Nouvel identifiant de session après un changement d'identifiant d'authentification (fixation de session)
        http.changeSessionId();
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("mes-donnees-ezproject.json").build().toString())
                .body(service.exportMyData());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Valid @RequestBody DeleteRequest request, HttpServletRequest http) {
        service.deleteMyAccount(request.password());
        HttpSession session = http.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
