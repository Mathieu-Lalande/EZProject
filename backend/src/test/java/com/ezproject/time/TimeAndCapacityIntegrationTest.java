package com.ezproject.time;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.calendar.HolidayCalendar;
import com.ezproject.support.DemoData;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class TimeAndCapacityIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoData demo;

    private static MockHttpServletRequestBuilder as(String login, MockHttpServletRequestBuilder b) {
        return b.with(user(login + "@ezproject.local")).with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    /** Un jour ouvré passé sans temps saisi dans le jeu de démo (il y a plus de 3 mois). */
    private static LocalDate freeWorkingDay() {
        LocalDate d = LocalDate.now().minusMonths(4);
        while (!HolidayCalendar.FR.isWorkingDay(d)) {
            d = d.minusDays(1);
        }
        return d;
    }

    @Test
    void logTimeWithBusinessRules() throws Exception {
        long portail = demo.project("PORTAIL");
        LocalDate day = freeWorkingDay();
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/time"))
                        .content("{\"date\":\"" + day + "\",\"days\":0.75,\"comment\":\"Revue d'accessibilité\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("Sarah Petit"));
        // Plus d'une journée le même jour
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/time")).content("{\"date\":\"" + day + "\",\"days\":0.5}"))
                .andExpect(status().isConflict());
        // Pas de saisie dans le futur, pas de fraction autre qu'une heure
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/time"))
                        .content("{\"date\":\"" + LocalDate.now().plusDays(3) + "\",\"days\":0.5}"))
                .andExpect(status().isConflict());
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/time")).content("{\"date\":\"" + day.minusDays(7) + "\",\"days\":0.3}"))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyManagersLogTimeForOthersAndViewersDoNotSeeBudget() throws Exception {
        long portail = demo.project("PORTAIL");
        String forHugo = "{\"date\":\"" + freeWorkingDay() + "\",\"days\":0.5,\"userId\":" + demo.user("hugo") + "}";
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/time")).content(forHugo)).andExpect(status().isForbidden());
        mvc.perform(as("lea", post("/api/projects/" + portail + "/time")).content(forHugo)).andExpect(status().isCreated());
        mvc.perform(as("julie", get("/api/projects/" + portail + "/time/summary"))).andExpect(status().isForbidden());
    }

    @Test
    void budgetSummaryAndPortfolioDrift() throws Exception {
        long audit = demo.project("AUDITSI");
        mvc.perform(as("thomas", get("/api/projects/" + audit + "/time/summary")))
                .andExpect(jsonPath("$.budgetDays").value(40))
                .andExpect(jsonPath("$.consumedDays").value(21))
                .andExpect(jsonPath("$.consumptionPct").value(53))
                .andExpect(jsonPath("$.byUser[0].name").value("Nina Lefèvre"))
                .andExpect(jsonPath("$.weeklyBurn").value(greaterThan(0.0)));
        mvc.perform(as("lea", get("/api/portfolio")))
                .andExpect(jsonPath("$.projects[?(@.code=='AUDITSI')].budgetPct").value(hasItem(53)))
                .andExpect(jsonPath("$.projects[?(@.code=='AUDITSI')].atRisk").value(hasItem(true)));
    }

    @Test
    void weeklyTimesheetReplacesCells() throws Exception {
        LocalDate monday = LocalDate.now().minusWeeks(20).with(DayOfWeek.MONDAY);
        while (!HolidayCalendar.FR.isWorkingDay(monday)) {
            monday = monday.minusWeeks(1);
        }
        long portail = demo.project("PORTAIL");
        String body = "{\"weekStart\":\"" + monday + "\",\"cells\":[{\"projectId\":" + portail + ",\"date\":\"" + monday
                + "\",\"days\":0.5}]}";
        mvc.perform(as("hugo", put("/api/time/week")).content(body))
                .andExpect(jsonPath("$.cells[0].days").value(0.5))
                .andExpect(jsonPath("$.projects[*].code").value(hasItem("PORTAIL")));
        mvc.perform(as("hugo", put("/api/time/week")).content(body.replace("0.5", "0")))
                .andExpect(jsonPath("$.cells.length()").value(0));
    }

    @Test
    void capacityScopeAndOverload() throws Exception {
        // Manager : tout le monde ; Hugo est surchargé cette semaine (5,5 j affectés pour 5)
        mvc.perform(as("lea", get("/api/capacity").param("weeks", "2")))
                .andExpect(jsonPath("$.people[?(@.displayName=='Hugo Bernard')].weeks[0].loadPct").value(hasItem(greaterThan(100))))
                .andExpect(jsonPath("$.people[*].displayName").value(not(hasItem("Julie Moreau"))));
        // Responsable d'équipe : son équipe uniquement
        mvc.perform(as("hugo", get("/api/capacity").param("weeks", "1")))
                .andExpect(jsonPath("$.people[*].displayName").value(hasItem("Sarah Petit")))
                .andExpect(jsonPath("$.people[*].displayName").value(not(hasItem("Thomas Roux"))));
        // Membre : uniquement soi
        mvc.perform(as("sarah", get("/api/capacity").param("weeks", "1")))
                .andExpect(jsonPath("$.people.length()").value(1));
    }

    @Test
    void absenceReasonIsPrivate() throws Exception {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusWeeks(4);
        mvc.perform(as("hugo", get("/api/absences").param("from", from.toString()).param("to", to.toString())))
                .andExpect(jsonPath("$[?(@.userName=='Sarah Petit')].kind").value(everyItem(nullValue())));
        mvc.perform(as("sarah", get("/api/absences/mine"))).andExpect(jsonPath("$[0].kind").value("VACATION"));
        mvc.perform(as("camille", get("/api/absences").param("from", from.toString()).param("to", to.toString())))
                .andExpect(jsonPath("$[?(@.userName=='Sarah Petit')].kind").value(hasItem("VACATION")));
        // Un membre ne déclare pas d'absence pour quelqu'un d'autre
        mvc.perform(as("sarah", post("/api/absences")).content("{\"startDate\":\"" + to + "\",\"endDate\":\"" + to
                + "\",\"kind\":\"OTHER\",\"userId\":" + demo.user("hugo") + "}")).andExpect(status().isForbidden());
    }

    @Test
    void staffingRequiresTimeManage() throws Exception {
        long portail = demo.project("PORTAIL");
        String body = "{\"userId\":" + demo.user("sarah") + ",\"startDate\":\"" + LocalDate.now() + "\",\"endDate\":\""
                + LocalDate.now().plusWeeks(2) + "\",\"daysPerWeek\":1}";
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/allocations")).content(body)).andExpect(status().isForbidden());
        mvc.perform(as("hugo", post("/api/projects/" + portail + "/allocations")).content(body)).andExpect(status().isCreated());
        mvc.perform(as("hugo", get("/api/projects/" + portail + "/allocations")))
                .andExpect(jsonPath("$[*].userName").value(hasItem("Sarah Petit")));
    }
}
