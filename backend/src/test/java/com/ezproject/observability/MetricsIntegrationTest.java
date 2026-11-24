package com.ezproject.observability;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.metrics.token=jeton-de-test")
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("dev")
class MetricsIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void prometheusEndpointRequiresToken() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).with(r -> { r.setRemoteAddr("10.7.0.1"); return r; })
                .param("email", "x@x.test").param("password", "faux")).andExpect(status().isUnauthorized());

        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer mauvais")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer jeton-de-test"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ezproject_auth_login_total{")))
                .andExpect(content().string(containsString("outcome=\"failure\"")))
                .andExpect(content().string(containsString("ezproject_sse_connections")));
    }

    @Test
    void healthProbesArePublic() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }
}
