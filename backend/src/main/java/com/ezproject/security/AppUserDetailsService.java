package com.ezproject.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ezproject.user.AppUserRepository;

@Service
class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    AppUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return repository.findByEmailIgnoreCase(email)
                .map(u -> new TenantPrincipal(u.getEmail(), u.getPasswordHash(),
                        // Compte désactivé ou supprimé : connexion refusée (DisabledException -> 401)
                        u.isActive() && !u.isDeleted(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())),
                        u.getOrganization().getId()))
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
    }
}
