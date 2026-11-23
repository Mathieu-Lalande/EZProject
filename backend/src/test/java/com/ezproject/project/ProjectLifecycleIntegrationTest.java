package com.ezproject.project;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.support.DemoData;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ProjectLifecycleIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoData demo;

    @Test
    void puttingOnHoldRequiresReasonAndIsJournaled() throws Exception {
        long mobile = demo.project("MOBILE");
        mvc.perform(patch("/api/projects/" + mobile + "/status").with(user("hugo@ezproject.local")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ON_HOLD\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/projects/" + mobile + "/status").with(user("hugo@ezproject.local")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_HOLD\",\"reason\":\"Arbitrage staffing\"}"))
                .andExpect(jsonPath("$.status").value("ON_HOLD"))
                .andExpect(jsonPath("$.statusReason").value("Arbitrage staffing"));
        mvc.perform(get("/api/projects/" + mobile + "/history").with(user("hugo@ezproject.local")))
                .andExpect(jsonPath("$[0].toStatus").value("ON_HOLD"))
                .andExpect(jsonPath("$[0].fromStatus").value("ACTIVE"))
                .andExpect(jsonPath("$[0].changedBy").value("Hugo Bernard"));
    }

    @Test
    void viewerCannotChangePriority() throws Exception {
        long mobile = demo.project("MOBILE");
        mvc.perform(patch("/api/projects/" + mobile + "/priority").with(user("nina@ezproject.local")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"priority\":\"P1\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/projects/" + mobile + "/priority").with(user("lea@ezproject.local")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"priority\":\"P1\"}"))
                .andExpect(jsonPath("$.priority").value("P1"));
    }

    @Test
    void signupCreatesIsolatedOrganizationOnFreePlanWithQuota() throws Exception {
        mvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"organizationName":"Nouvelle Boîte","displayName":"Alice","email":"alice@nouvelle.test",
                         "password":"un-mot-de-passe-solide"}"""))
                .andExpect(status().isCreated());
        var alice = user("alice@nouvelle.test");
        mvc.perform(get("/api/organization").with(alice))
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.slug").value("nouvelle-boite"))
                .andExpect(jsonPath("$.usage.maxProjects").value(3));
        mvc.perform(get("/api/projects").with(alice)).andExpect(jsonPath("$.length()").value(0));
        for (String code : new String[] {"AA", "BB", "CC"}) {
            mvc.perform(post("/api/projects").with(alice).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"" + code + "\",\"name\":\"P\",\"type\":\"INTERNAL\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/projects").with(alice).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DD\",\"name\":\"P\",\"type\":\"INTERNAL\"}"))
                .andExpect(status().isConflict());
        // Le même code projet est libre dans une autre organisation
        mvc.perform(post("/api/projects").with(user("lea@ezproject.local")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"AA\",\"name\":\"P\",\"type\":\"INTERNAL\"}"))
                .andExpect(status().isCreated());
    }
}
