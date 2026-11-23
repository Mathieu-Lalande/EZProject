package com.ezproject.user;

/** Représentation publique d'un utilisateur (jamais le hash du mot de passe). */
public record UserDto(Long id, String email, String displayName, Role role, String jobTitle, boolean active) {

    public static UserDto from(AppUser u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getJobTitle(), u.isActive());
    }
}
