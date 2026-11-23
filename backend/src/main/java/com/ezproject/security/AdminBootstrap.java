package com.ezproject.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ezproject.organization.OrganizationRepository;
import com.ezproject.organization.OrganizationService;
import com.ezproject.organization.Plan;
import com.ezproject.organization.SignupRequest;
import com.ezproject.user.AppUserRepository;

/**
 * Installation auto-hébergée : crée la première organisation (offre ENTERPRISE) et son propriétaire
 * à partir de variables d'environnement, si la base ne contient encore aucun utilisateur.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppUserRepository users;
    private final OrganizationService organizations;
    private final OrganizationRepository organizationRepository;
    private final String email;
    private final String password;
    private final String organizationName;

    AdminBootstrap(AppUserRepository users, OrganizationService organizations,
                   OrganizationRepository organizationRepository,
                   @Value("${app.bootstrap-admin.email:}") String email,
                   @Value("${app.bootstrap-admin.password:}") String password,
                   @Value("${app.bootstrap-admin.organization:Mon organisation}") String organizationName) {
        this.users = users;
        this.organizations = organizations;
        this.organizationRepository = organizationRepository;
        this.email = email;
        this.password = password;
        this.organizationName = organizationName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password) || users.count() > 0) {
            return;
        }
        if (password.length() < 12) {
            log.warn("ADMIN_PASSWORD trop court (12 caractères minimum) : administrateur non créé");
            return;
        }
        var org = organizations.signup(new SignupRequest(organizationName, "Administrateur", email, password));
        organizationRepository.findById(org.getId()).ifPresent(o -> o.changePlan(Plan.ENTERPRISE));
        log.info("Organisation initiale et propriétaire créés");
    }
}
