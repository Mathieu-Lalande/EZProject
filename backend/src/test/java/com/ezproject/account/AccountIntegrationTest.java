package com.ezproject.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.project.ProjectRepository;
import com.ezproject.support.DemoData;
import com.ezproject.user.AppUserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AccountIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoData demo;

    @Autowired
    AppUserRepository users;

    @Autowired
    ProjectRepository projects;

    private static RequestPostProcessor ip(String address) {
        return r -> {
            r.setRemoteAddr(address);
            return r;
        };
    }

    private MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login").with(csrf()).with(ip("10.9.0.1")).param("email", email).param("password", password);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String login, String body) {
        return b.with(user(login + "@ezproject.local")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void userChangesOwnPasswordAfterCheckingCurrentOne() throws Exception {
        mvc.perform(json(put("/api/account/password"), "nina", "{\"currentPassword\":\"faux\",\"newPassword\":\"nouveau-mot-de-passe\"}"))
                .andExpect(status().isConflict());
        mvc.perform(json(put("/api/account/password"), "nina", "{\"currentPassword\":\"demo1234\",\"newPassword\":\"court\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/account/password"), "nina", "{\"currentPassword\":\"demo1234\",\"newPassword\":\"nouveau-mot-de-passe\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(login("nina@ezproject.local", "nouveau-mot-de-passe")).andExpect(status().isNoContent());
        mvc.perform(login("nina@ezproject.local", "demo1234")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminResetForcesPasswordChangeBeforeAnythingElse() throws Exception {
        String body = mvc.perform(json(post("/api/users/" + demo.user("sarah") + "/reset-password"), "camille", ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String temporary = body.replaceAll(".*\"temporaryPassword\":\"([^\"]+)\".*", "$1");
        assertThat(temporary).hasSize(16);

        mvc.perform(login("sarah@ezproject.local", temporary)).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects").with(user("sarah@ezproject.local")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        mvc.perform(get("/api/auth/me").with(user("sarah@ezproject.local")))
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        mvc.perform(json(put("/api/account/password"), "sarah",
                        "{\"currentPassword\":\"" + temporary + "\",\"newPassword\":\"mon-propre-mot-de-passe\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/projects").with(user("sarah@ezproject.local"))).andExpect(status().isOk());
    }

    @Test
    void deactivatedUserIsLockedOutImmediately() throws Exception {
        long hugo = demo.user("hugo");
        mvc.perform(json(put("/api/users/" + hugo + "/active"), "sarah", "{\"active\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(json(put("/api/users/" + hugo + "/active"), "camille", "{\"active\":false}"))
                .andExpect(jsonPath("$.active").value(false));
        // Session existante coupée
        mvc.perform(get("/api/projects").with(user("hugo@ezproject.local")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        // Nouvelle connexion refusée
        mvc.perform(login("hugo@ezproject.local", "demo1234")).andExpect(status().isUnauthorized());
        mvc.perform(json(put("/api/users/" + hugo + "/active"), "camille", "{\"active\":true}"))
                .andExpect(jsonPath("$.active").value(true));
        mvc.perform(login("hugo@ezproject.local", "demo1234")).andExpect(status().isNoContent());
    }

    @Test
    void adminDeletionAnonymizesButKeepsProjectHistory() throws Exception {
        long nina = demo.user("nina");
        mvc.perform(json(delete("/api/users/" + nina), "camille", "")).andExpect(status().isNoContent());
        var anonymized = users.findById(nina).orElseThrow();
        assertThat(anonymized.getEmail()).isEqualTo("supprime-" + nina + "@deleted.invalid");
        assertThat(anonymized.getDisplayName()).isEqualTo("Utilisateur supprimé");
        assertThat(anonymized.isActive()).isFalse();
        mvc.perform(get("/api/users").with(user("camille@ezproject.local")))
                .andExpect(jsonPath("$[*].id").value(not(hasItem((int) nina))));
    }

    @Test
    void personalDataExportAndSelfDeletion() throws Exception {
        mvc.perform(get("/api/account/export").with(user("thomas@ezproject.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.email").value("thomas@ezproject.local"))
                .andExpect(jsonPath("$.teams[0].team").value("Conseil SI"));
        mvc.perform(json(delete("/api/account"), "thomas", "{\"password\":\"mauvais\"}")).andExpect(status().isConflict());
        mvc.perform(json(delete("/api/account"), "thomas", "{\"password\":\"demo1234\"}")).andExpect(status().isNoContent());
        assertThat(users.findByEmailIgnoreCase("thomas@ezproject.local")).isEmpty();
        // Le seul propriétaire ne peut pas supprimer son compte sans transmettre l'organisation
        mvc.perform(json(delete("/api/account"), "camille", "{\"password\":\"demo1234\"}")).andExpect(status().isConflict());
    }

    @Test
    void profileUpdate() throws Exception {
        mvc.perform(json(put("/api/account/profile"), "lea", "{\"displayName\":\"Léa Dubois-Martin\",\"jobTitle\":\"Directrice PMO\"}"))
                .andExpect(jsonPath("$.displayName").value("Léa Dubois-Martin"))
                .andExpect(jsonPath("$.jobTitle").value("Directrice PMO"));
    }

    @Test
    void ownerDeletesOrganizationWithNameConfirmation() throws Exception {
        mvc.perform(post("/api/auth/signup").with(csrf()).with(ip("10.9.0.2")).contentType(MediaType.APPLICATION_JSON).content("""
                {"organizationName":"Société Éphémère","displayName":"Eve","email":"eve@ephemere.test",
                 "password":"un-mot-de-passe-solide"}""")).andExpect(status().isCreated());
        var eve = user("eve@ephemere.test");
        mvc.perform(post("/api/projects").with(eve).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"TMP\",\"name\":\"Temporaire\",\"type\":\"INTERNAL\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/organization/export").with(eve))
                .andExpect(jsonPath("$.projects[0].code").value("TMP"))
                .andExpect(jsonPath("$.users[0].email").value("eve@ephemere.test"));

        mvc.perform(delete("/api/organization").with(eve).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationName\":\"Mauvais nom\"}")).andExpect(status().isConflict());
        mvc.perform(delete("/api/organization").with(eve).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationName\":\"Société Éphémère\"}")).andExpect(status().isNoContent());
        assertThat(users.findByEmailIgnoreCase("eve@ephemere.test")).isEmpty();
        assertThat(projects.findAll()).noneMatch(p -> p.getCode().equals("TMP"));
    }

    @Test
    void auditLogTracksActionsAndIsAdminOnly() throws Exception {
        long mobile = demo.project("MOBILE");
        mvc.perform(json(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/projects/" + mobile + "/priority"), "lea", "{\"priority\":\"P1\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/organization/audit").with(user("camille@ezproject.local")).param("q", "mobile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("PROJECT_PRIORITY"))
                .andExpect(jsonPath("$.content[0].actorName").value("Léa Dubois"))
                .andExpect(jsonPath("$.totalElements").isNumber());
        mvc.perform(get("/api/organization/audit").with(user("lea@ezproject.local"))).andExpect(status().isForbidden());
        // Isolation : l'audit d'Acme ne contient rien d'Horizon
        mvc.perform(get("/api/organization/audit").with(user("bob@acme.local")).param("q", "mobile"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
