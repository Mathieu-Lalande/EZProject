package com.ezproject.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ezproject.user.AppUser;
import com.ezproject.user.AppUserRepository;
import com.ezproject.user.Role;

/** Crée le premier administrateur à partir de variables d'environnement si la base est vide. */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    AdminBootstrap(AppUserRepository repository, PasswordEncoder passwordEncoder,
                   @Value("${app.bootstrap-admin.email:}") String email,
                   @Value("${app.bootstrap-admin.password:}") String password) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password) || repository.count() > 0) {
            return;
        }
        if (password.length() < 12) {
            log.warn("ADMIN_PASSWORD trop court (12 caractères minimum) : administrateur non créé");
            return;
        }
        repository.save(new AppUser(email, "Administrateur", passwordEncoder.encode(password), Role.ADMIN, null));
        log.info("Administrateur initial créé");
    }
}
