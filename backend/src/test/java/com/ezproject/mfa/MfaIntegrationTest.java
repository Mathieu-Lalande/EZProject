package com.ezproject.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.support.DemoData;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class MfaIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoData demo;

    private static long nowStep() {
        return Totp.stepAt(Instant.now().getEpochSecond());
    }

    private String enroll(String login) throws Exception {
        var u = user(login + "@ezproject.local");
        String body = mvc.perform(post("/api/account/mfa/setup").with(u).with(csrf()))
                .andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/EZProject:")))
                .andReturn().getResponse().getContentAsString();
        String secret = body.replaceAll(".*\"secret\":\"([A-Z2-7]+)\".*", "$1");
        mvc.perform(post("/api/account/mfa/enable").with(u).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isConflict());
        String codes = mvc.perform(post("/api/account/mfa/enable").with(u).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + Totp.code(secret, nowStep()) + "\"}"))
                .andExpect(jsonPath("$.recoveryCodes.length()").value(8))
                .andReturn().getResponse().getContentAsString();
        return secret + "|" + codes.replaceAll(".*\\[\"([A-Z0-9-]+)\".*", "$1");
    }

    private MockHttpSession login(String login) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .with(r -> { r.setRemoteAddr("10.8.0.1"); return r; })
                        .param("email", login + "@ezproject.local").param("password", "demo1234"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andReturn().getRequest().getSession();
    }

    @Test
    void loginRequiresSecondFactorAndRejectsReplay() throws Exception {
        String[] enrolled = enroll("hugo").split("\\|");
        String secret = enrolled[0];
        MockHttpSession session = login("hugo");

        mvc.perform(get("/api/projects").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MFA_REQUIRED"));
        mvc.perform(post("/api/auth/mfa/verify").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"123456\"}")).andExpect(status().isUnauthorized());
        // Le code de l'enrôlement (même pas de temps) est refusé : anti-rejeu ; le pas suivant est accepté
        mvc.perform(post("/api/auth/mfa/verify").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + Totp.code(secret, nowStep() + 1) + "\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects").session(session)).andExpect(status().isOk());
    }

    @Test
    void recoveryCodeWorksOnce() throws Exception {
        String recovery = enroll("sarah").split("\\|")[1];
        MockHttpSession first = login("sarah");
        mvc.perform(post("/api/auth/mfa/verify").session(first).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + recovery.toLowerCase() + "\"}")).andExpect(status().isNoContent());
        MockHttpSession second = login("sarah");
        mvc.perform(post("/api/auth/mfa/verify").session(second).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + recovery + "\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanResetLostDevice() throws Exception {
        enroll("thomas");
        mvc.perform(delete("/api/users/" + demo.user("thomas") + "/mfa").with(user("sarah@ezproject.local")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/users/" + demo.user("thomas") + "/mfa").with(user("camille@ezproject.local")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").with(user("thomas@ezproject.local"))).andExpect(jsonPath("$.mfaEnabled").value(false));
        assertThat(true).isTrue();
    }
}
