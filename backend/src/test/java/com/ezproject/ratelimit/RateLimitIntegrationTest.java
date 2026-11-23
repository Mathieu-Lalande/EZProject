package com.ezproject.ratelimit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

/** Limites volontairement basses pour éprouver chaque protection ; chaque test utilise sa propre IP. */
@SpringBootTest(properties = {
        "app.rate-limit.login-ip.capacity=5",
        "app.rate-limit.login-account.capacity=3",
        "app.rate-limit.signup-ip.capacity=2",
        "app.rate-limit.api-user.capacity=5",
        "app.rate-limit.api-anonymous.capacity=3",
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RateLimitIntegrationTest {

    @Autowired
    MockMvc mvc;

    private static RequestPostProcessor ip(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static MockHttpServletRequestBuilder login(String email, String password, String address) {
        return post("/api/auth/login").with(csrf()).with(ip(address)).param("email", email).param("password", password);
    }

    @Test
    void accountIsLockedAfterRepeatedFailuresEvenWithTheRightPassword() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(login("sarah@ezproject.local", "mauvais", "10.0.0." + i)).andExpect(status().isUnauthorized());
        }
        // Depuis une autre IP et avec le bon mot de passe : le compte reste bloqué (force brute distribuée)
        mvc.perform(login("sarah@ezproject.local", "demo1234", "10.0.0.99"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail").value(containsString("Réessayez")));
        // Les autres comptes ne sont pas affectés
        mvc.perform(login("nina@ezproject.local", "demo1234", "10.0.0.98")).andExpect(status().isNoContent());
    }

    @Test
    void successfulLoginResetsFailureCounter() throws Exception {
        mvc.perform(login("thomas@ezproject.local", "mauvais", "10.0.1.1")).andExpect(status().isUnauthorized());
        mvc.perform(login("thomas@ezproject.local", "mauvais", "10.0.1.1")).andExpect(status().isUnauthorized());
        mvc.perform(login("thomas@ezproject.local", "demo1234", "10.0.1.1")).andExpect(status().isNoContent());
        mvc.perform(login("thomas@ezproject.local", "mauvais", "10.0.1.1")).andExpect(status().isUnauthorized());
        mvc.perform(login("thomas@ezproject.local", "demo1234", "10.0.1.1")).andExpect(status().isNoContent());
    }

    @Test
    void loginAttemptsAreLimitedPerIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(login("inconnu" + i + "@x.test", "x", "10.0.2.1")).andExpect(status().isUnauthorized());
        }
        mvc.perform(login("hugo@ezproject.local", "demo1234", "10.0.2.1")).andExpect(status().isTooManyRequests());
        mvc.perform(login("hugo@ezproject.local", "demo1234", "10.0.2.2")).andExpect(status().isNoContent());
    }

    @Test
    void signupIsLimitedPerIp() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/auth/signup").with(csrf()).with(ip("10.0.3.1"))
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/auth/signup").with(csrf()).with(ip("10.0.3.1"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isTooManyRequests());
    }

    @Test
    void apiIsLimitedPerUserAndAnonymousPerIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/projects").with(user("lea@ezproject.local"))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/projects").with(user("lea@ezproject.local"))).andExpect(status().isTooManyRequests());
        mvc.perform(get("/api/projects").with(user("camille@ezproject.local"))).andExpect(status().isOk());

        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/auth/me").with(ip("10.0.4.1"))).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/auth/me").with(ip("10.0.4.1"))).andExpect(status().isTooManyRequests());
    }
}
