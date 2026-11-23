package com.ezproject.security;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.ezproject.todo.TodoRepository;
import com.ezproject.user.AppUserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ApiSecurityIntegrationTest {

    private static final String NEW_PROJECT = """
            {"code":"NEWPRJ","name":"Nouveau","type":"CONSULTING"}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    TodoRepository todos;

    @Autowired
    AppUserRepository users;

    @Test
    void anonymousAccessIsRejectedWith401() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithValidCredentialsReturns204AndInvalidReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", "hugo@ezproject.local").param("password", "demo1234"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", "hugo@ezproject.local").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Contexte neuf : le post-processor csrf() de spring-security-test remplace durablement le repository CSRF
     * du filtre partagé, ce qui empêcherait d'observer le vrai cookie XSRF-TOKEN.
     */
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void csrfCookieIsIssuedAndMutationWithoutTokenIsForbidden() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(cookie().exists("XSRF-TOKEN"));
        mvc.perform(post("/api/auth/login")
                        .param("email", "hugo@ezproject.local").param("password", "demo1234"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hugo@ezproject.local", roles = "MEMBER")
    void memberCannotCreateProject() throws Exception {
        mvc.perform(post("/api/projects").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(NEW_PROJECT))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hugo@ezproject.local", roles = "MEMBER")
    void memberCannotSeePortfolio() throws Exception {
        mvc.perform(get("/api/portfolio")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "lea@ezproject.local", roles = "MANAGER")
    void managerSeesPortfolio() throws Exception {
        mvc.perform(get("/api/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.openProjects").isNumber());
    }

    @Test
    @WithMockUser(username = "lea@ezproject.local", roles = "MANAGER")
    void invalidProjectIsRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/projects").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad code\",\"name\":\"\",\"type\":\"CONSULTING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @WithMockUser(username = "hugo@ezproject.local", roles = "MEMBER")
    void personalTodoOfAnotherUserIsNotAccessible() throws Exception {
        Long camilleId = users.findByEmailIgnoreCase("camille@ezproject.local").orElseThrow().getId();
        Long foreignTodo = todos.findByOwnerIdAndProjectIsNullOrderByDoneAscCreatedAtDesc(camilleId)
                .getFirst().getId();

        mvc.perform(put("/api/todos/" + foreignTodo).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"piraté\",\"done\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "hugo@ezproject.local", roles = "MEMBER")
    void pokerCardsAreHiddenUntilReveal() throws Exception {
        String body = mvc.perform(post("/api/projects/1/poker/rounds").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"storyTitle\":\"Story test\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String roundId = body.replaceAll(".*?\"id\":(\\d+).*", "$1");

        mvc.perform(post("/api/poker/rounds/" + roundId + "/vote").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"card\":\"5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myCard").value("5"))
                .andExpect(jsonPath("$.votes[0].card").value(nullValue()))
                .andExpect(jsonPath("$.stats").value(nullValue()));
    }
}
