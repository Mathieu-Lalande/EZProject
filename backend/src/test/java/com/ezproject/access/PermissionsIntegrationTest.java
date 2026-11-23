package com.ezproject.access;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.support.DemoData;
import com.ezproject.task.TaskRepository;

/**
 * Rôles et permissions sur le jeu de démo "Horizon Conseil" :
 * Delivery Web (hugo LEAD, sarah CONTRIBUTOR), Conseil SI (thomas LEAD, nina CONTRIBUTOR),
 * julie invitée en lecture sur PORTAIL, MANDA confidentiel (thomas ajouté explicitement).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class PermissionsIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoData demo;

    @Autowired
    TaskRepository tasks;

    private static MockHttpServletRequestBuilder as(String login, MockHttpServletRequestBuilder req) {
        String domain = login.equals("bob") ? "acme.local" : "ezproject.local";
        return req.with(user(login + "@" + domain)).with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    private static final String NEW_TASK = "{\"title\":\"Nouvelle tâche\"}";

    @Test
    void guestSeesOnlyProjectsWhereInvitedAndReadOnly() throws Exception {
        long portail = demo.project("PORTAIL");
        mvc.perform(as("julie", get("/api/projects")))
                .andExpect(jsonPath("$[*].code").value(containsInAnyOrder("PORTAIL")))
                .andExpect(jsonPath("$[0].myRole").value("VIEWER"));
        mvc.perform(as("julie", get("/api/projects/" + portail + "/tasks"))).andExpect(status().isOk());
        mvc.perform(as("julie", post("/api/projects/" + portail + "/tasks")).content(NEW_TASK))
                .andExpect(status().isForbidden());
        mvc.perform(as("julie", get("/api/projects/" + demo.project("MOBILE")))).andExpect(status().isNotFound());
    }

    @Test
    void teamVisibilityHidesProjectFromOtherTeams() throws Exception {
        mvc.perform(as("nina", get("/api/projects/" + demo.project("PORTAIL")))).andExpect(status().isNotFound());
        // Projet ouvert à l'organisation : lecture seule pour une autre équipe
        long mobile = demo.project("MOBILE");
        mvc.perform(as("nina", get("/api/projects/" + mobile))).andExpect(jsonPath("$.myRole").value("VIEWER"));
        mvc.perform(as("nina", post("/api/projects/" + mobile + "/tasks")).content(NEW_TASK))
                .andExpect(status().isForbidden());
    }

    @Test
    void restrictedProjectOnlyForExplicitMembersAndManagers() throws Exception {
        long manda = demo.project("MANDA");
        mvc.perform(as("thomas", get("/api/projects/" + manda))).andExpect(jsonPath("$.myRole").value("CONTRIBUTOR"));
        mvc.perform(as("nina", get("/api/projects/" + manda))).andExpect(status().isNotFound());
        mvc.perform(as("hugo", get("/api/projects"))).andExpect(jsonPath("$[*].code").value(not(hasItem("MANDA"))));
        mvc.perform(as("lea", get("/api/projects/" + manda))).andExpect(jsonPath("$.myRole").value("LEAD"));
    }

    @Test
    void contributorCreatesButCannotDeleteTasks() throws Exception {
        long portail = demo.project("PORTAIL");
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/tasks")).content(NEW_TASK))
                .andExpect(status().isCreated());
        long taskId = tasks.findByProjectIdOrderByPositionAsc(portail).getFirst().getId();
        mvc.perform(as("sarah", delete("/api/tasks/" + taskId))).andExpect(status().isForbidden());
        mvc.perform(as("hugo", delete("/api/tasks/" + taskId))).andExpect(status().isNoContent());
    }

    @Test
    void projectCreationRequiresManagerOrTeamLead() throws Exception {
        String inDelivery = "{\"code\":\"NEWDEV\",\"name\":\"Nouveau\",\"type\":\"DEVELOPMENT\",\"teamId\":"
                + demo.team("Delivery Web") + "}";
        mvc.perform(as("sarah", post("/api/projects")).content(inDelivery)).andExpect(status().isForbidden());
        mvc.perform(as("hugo", post("/api/projects")).content(inDelivery)).andExpect(status().isCreated());
        // Hugo n'est pas responsable de l'équipe Conseil SI
        String inConseil = inDelivery.replace("NEWDEV", "NEWCSL").replace(
                String.valueOf(demo.team("Delivery Web")), String.valueOf(demo.team("Conseil SI")));
        mvc.perform(as("hugo", post("/api/projects")).content(inConseil)).andExpect(status().isForbidden());
    }

    @Test
    void customPermissionMatrixIsEnforced() throws Exception {
        long portail = demo.project("PORTAIL");
        String matrix = """
                {"roles":{
                  "LEAD":["PROJECT_EDIT","PROJECT_MODULES","PROJECT_MEMBERS","TASK_VIEW","TASK_CREATE","TASK_EDIT","TASK_DELETE"],
                  "CONTRIBUTOR":["TASK_VIEW","TASK_EDIT"],
                  "VIEWER":["TASK_VIEW"]}}
                """;
        mvc.perform(as("sarah", put("/api/organization/permissions")).content(matrix)).andExpect(status().isForbidden());
        mvc.perform(as("camille", put("/api/organization/permissions")).content(matrix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customized").value(true));
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/tasks")).content(NEW_TASK))
                .andExpect(status().isForbidden());
        mvc.perform(as("sarah", get("/api/projects/" + portail + "/retro-plan"))).andExpect(status().isForbidden());

        mvc.perform(as("camille", delete("/api/organization/permissions"))).andExpect(jsonPath("$.customized").value(false));
        mvc.perform(as("sarah", post("/api/projects/" + portail + "/tasks")).content(NEW_TASK))
                .andExpect(status().isCreated());
    }

    @Test
    void leadCanInviteGuestAsViewerButNotAsLead() throws Exception {
        long portail = demo.project("PORTAIL");
        long julie = demo.user("julie");
        mvc.perform(as("hugo", put("/api/projects/" + portail + "/members/" + julie)).content("{\"role\":\"LEAD\"}"))
                .andExpect(status().isConflict());
        mvc.perform(as("hugo", put("/api/projects/" + portail + "/members/" + julie)).content("{\"role\":\"CONTRIBUTOR\"}"))
                .andExpect(jsonPath("$.explicitMembers[0].role").value("CONTRIBUTOR"));
        mvc.perform(as("sarah", put("/api/projects/" + portail + "/members/" + julie)).content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberPortfolioIsScopedToVisibleProjects() throws Exception {
        mvc.perform(as("nina", get("/api/portfolio")))
                .andExpect(jsonPath("$.scope").value("MY_PROJECTS"))
                .andExpect(jsonPath("$.projects[*].code").value(not(hasItem("PORTAIL"))))
                .andExpect(jsonPath("$.projects[*].code").value(not(hasItem("MANDA"))));
        mvc.perform(as("lea", get("/api/portfolio")))
                .andExpect(jsonPath("$.scope").value("ORGANIZATION"))
                .andExpect(jsonPath("$.kpis.onHoldProjects").value(1))
                // Tri par priorité : un P1 en tête
                .andExpect(jsonPath("$.projects[0].priority").value("P1"));
    }

    @Test
    void organizationsAreIsolated() throws Exception {
        long erp = demo.project("ERP");
        long portail = demo.project("PORTAIL");
        mvc.perform(as("camille", get("/api/projects/" + erp))).andExpect(status().isNotFound());
        mvc.perform(as("bob", get("/api/projects/" + portail))).andExpect(status().isNotFound());
        mvc.perform(as("bob", get("/api/projects"))).andExpect(jsonPath("$[*].code").value(containsInAnyOrder("ERP")));
        mvc.perform(as("bob", get("/api/users"))).andExpect(jsonPath("$.length()").value(1));
        // Impossible d'assigner une tâche à un utilisateur d'une autre organisation
        mvc.perform(as("bob", post("/api/projects/" + erp + "/tasks"))
                        .content("{\"title\":\"x\",\"assigneeId\":" + demo.user("camille") + "}"))
                .andExpect(status().isNotFound());
    }
}
