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
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.support.DemoData;
import com.ezproject.todo.TodoRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ApiSecurityIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TodoRepository todos;

    @Autowired
    DemoData demo;

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
    @WithMockUser(username = "camille@ezproject.local")
    void meExposesOrganizationAndTeams() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.organization.name").value("Horizon Conseil"))
                .andExpect(jsonPath("$.teams[0].teamName").value("Direction"));
    }

    @Test
    @WithMockUser(username = "lea@ezproject.local")
    void invalidProjectIsRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/projects").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad code\",\"name\":\"\",\"type\":\"CONSULTING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @WithMockUser(username = "hugo@ezproject.local")
    void personalTodoOfAnotherUserIsNotAccessible() throws Exception {
        Long foreignTodo = todos.findByOwnerIdAndProjectIsNullOrderByDoneAscCreatedAtDesc(demo.user("camille"))
                .getFirst().getId();

        mvc.perform(put("/api/todos/" + foreignTodo).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"piraté\",\"done\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "hugo@ezproject.local")
    void pokerCardsAreHiddenUntilReveal() throws Exception {
        String body = mvc.perform(post("/api/projects/" + demo.project("PORTAIL") + "/poker/rounds").with(csrf())
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
