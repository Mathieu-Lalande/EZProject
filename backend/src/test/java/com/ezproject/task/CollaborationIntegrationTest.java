package com.ezproject.task;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.support.DemoData;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class CollaborationIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoData demo;

    @Autowired
    TaskRepository tasks;

    private long paiementTask() {
        return tasks.findByProjectIdOrderByPositionAsc(demo.project("PORTAIL")).stream()
                .filter(t -> t.getTitle().startsWith("Intégration prestataire")).findFirst().orElseThrow().getId();
    }

    private static MockHttpServletRequestBuilder as(String login, MockHttpServletRequestBuilder b) {
        return b.with(user(login + "@ezproject.local")).with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void commentsFollowPermissions() throws Exception {
        long task = paiementTask();
        mvc.perform(as("julie", post("/api/tasks/" + task + "/comments")).content("{\"body\":\"Question client\"}"))
                .andExpect(status().isForbidden());
        String created = mvc.perform(as("sarah", post("/api/tasks/" + task + "/comments")).content("{\"body\":\"Je prends les webhooks\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*?\"id\":(\\d+).*", "$1");
        mvc.perform(as("julie", get("/api/tasks/" + task + "/details")))
                .andExpect(jsonPath("$.comments[*].body").value(hasItem("Je prends les webhooks")))
                .andExpect(jsonPath("$.canComment").value(false));
        mvc.perform(as("hugo", put("/api/comments/" + id)).content("{\"body\":\"modifié\"}")).andExpect(status().isForbidden());
        mvc.perform(as("hugo", delete("/api/comments/" + id))).andExpect(status().isNoContent());
    }

    @Test
    void checklistAndLabelsAppearOnTheBoard() throws Exception {
        long task = paiementTask();
        long portail = demo.project("PORTAIL");
        mvc.perform(as("sarah", post("/api/tasks/" + task + "/checklist")).content("{\"title\":\"Recette paiement\",\"done\":false}"))
                .andExpect(jsonPath("$.length()").value(5));
        mvc.perform(as("sarah", get("/api/projects/" + portail + "/tasks")))
                .andExpect(jsonPath("$[?(@.id==" + task + ")].counts.checklistTotal").value(hasItem(5)))
                .andExpect(jsonPath("$[?(@.id==" + task + ")].counts.checklistDone").value(hasItem(2)))
                .andExpect(jsonPath("$[?(@.id==" + task + ")].counts.comments").value(hasItem(2)))
                .andExpect(jsonPath("$[?(@.id==" + task + ")].labels[0]").value(hasItem("Back")));
        mvc.perform(as("hugo", put("/api/tasks/" + task)).content("{\"title\":\"Intégration paiement\",\"labels\":[\"Sécurité\",\"sécurité\",\"Front\"]}"))
                .andExpect(jsonPath("$.labels.length()").value(2));
    }

    @Test
    void attachmentsAreValidatedAndDownloadedSafely() throws Exception {
        long task = paiementTask();
        var png = new MockMultipartFile("file", "../../maquette<1>.png", "text/html", PNG);
        String body = mvc.perform(multipart("/api/tasks/" + task + "/attachments").file(png).with(user("sarah@ezproject.local")).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("maquette_1_.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*?\"id\":(\\d+).*", "$1");

        mvc.perform(get("/api/attachments/" + id).with(user("julie@ezproject.local")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
        mvc.perform(get("/api/attachments/" + id).with(user("nina@ezproject.local"))).andExpect(status().isNotFound());

        var html = new MockMultipartFile("file", "page.html", "text/html", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/tasks/" + task + "/attachments").file(html).with(user("sarah@ezproject.local")).with(csrf()))
                .andExpect(status().isConflict());
        var fakePdf = new MockMultipartFile("file", "facture.pdf", "application/pdf", "pas un pdf".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/tasks/" + task + "/attachments").file(fakePdf).with(user("sarah@ezproject.local")).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(multipart("/api/tasks/" + task + "/attachments").file(png).with(user("julie@ezproject.local")).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
