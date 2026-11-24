package com.ezproject.report;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.support.DemoData;

/** Risques, décisions, météo historisée, rapport COPIL et roadmap. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class SteeringIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoData demo;

    private static MockHttpServletRequestBuilder as(String login, MockHttpServletRequestBuilder b) {
        return b.with(user(login + "@ezproject.local")).with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void risksAreReadableByViewersAndEditableByContributors() throws Exception {
        long portail = demo.project("PORTAIL");
        String critical = "{\"title\":\"Fuite de données de test\",\"probability\":4,\"impact\":4}";
        mvc.perform(as("julie", get("/api/projects/" + portail + "/risks"))).andExpect(status().isOk());
        mvc.perform(as("julie", post("/api/projects/" + portail + "/risks")).content(critical)).andExpect(status().isForbidden());
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/risks")).content(critical.replace("4,\"impact\"", "5,\"impact\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/risks")).content(critical))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(16))
                .andExpect(jsonPath("$.critical").value(true));
        mvc.perform(as("lea", get("/api/portfolio")))
                .andExpect(jsonPath("$.projects[?(@.code=='PORTAIL')].criticalRisks").value(hasItem(1)))
                .andExpect(jsonPath("$.projects[?(@.code=='PORTAIL')].atRisk").value(hasItem(true)));
    }

    @Test
    void publishingAReportUpdatesHealthAndHistory() throws Exception {
        long portail = demo.project("PORTAIL");
        String body = "{\"health\":\"AMBER\",\"trend\":\"DOWN\",\"summary\":\"Retard sur le paiement\"}";
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/reports")).content(body)).andExpect(status().isForbidden());
        mvc.perform(as("hugo", post("/api/projects/" + portail + "/reports")).content(body)).andExpect(status().isCreated());
        mvc.perform(as("hugo", get("/api/projects/" + portail))).andExpect(jsonPath("$.health").value("AMBER"));
        mvc.perform(as("hugo", get("/api/projects/" + portail + "/reports")))
                .andExpect(jsonPath("$[0].summary").value("Retard sur le paiement"))
                .andExpect(jsonPath("$[0].authorName").value("Hugo Bernard"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void portfolioFlagsStaleReports() throws Exception {
        mvc.perform(as("lea", get("/api/portfolio")))
                .andExpect(jsonPath("$.projects[?(@.code=='MOBILE')].reportStale").value(hasItem(true)))
                .andExpect(jsonPath("$.projects[?(@.code=='MOBILE')].latestReport.trend").value(hasItem("DOWN")))
                .andExpect(jsonPath("$.projects[?(@.code=='AUDITSI')].reportStale").value(hasItem(false)))
                .andExpect(jsonPath("$.kpis.criticalRisks").value(1));
    }

    @Test
    void copilReportRespectsPermissions() throws Exception {
        long portail = demo.project("PORTAIL");
        mvc.perform(as("julie", get("/api/projects/" + portail + "/copil")))
                .andExpect(jsonPath("$.latestReport.trend").value("UP"))
                .andExpect(jsonPath("$.risks").isArray())
                .andExpect(jsonPath("$.budget").doesNotExist());
        mvc.perform(as("lea", get("/api/projects/" + portail + "/copil")))
                .andExpect(jsonPath("$.budget.budgetDays").value(180))
                .andExpect(jsonPath("$.tasks.total").isNumber())
                .andExpect(jsonPath("$.decisions[0].title").value("Choix du prestataire de paiement"));
    }

    @Test
    void roadmapShowsVisibleProjectsWithPhases() throws Exception {
        mvc.perform(as("nina", get("/api/portfolio/roadmap")))
                .andExpect(jsonPath("$[*].code").value(not(hasItem("PORTAIL"))));
        mvc.perform(as("lea", get("/api/portfolio/roadmap")))
                .andExpect(jsonPath("$[?(@.code=='PORTAIL')].phases[0].name").value(hasItem("Cadrage")))
                .andExpect(jsonPath("$[*].code").value(not(hasItem("ERP"))));
    }
}
