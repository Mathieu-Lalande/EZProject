package com.ezproject.poker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ezproject.support.DemoData;

/** Pas de transaction de test : les événements sont émis après un vrai commit. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PokerStreamIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoData demo;

    @Test
    void votersReceiveSignalsWithoutCardValues() throws Exception {
        long portail = demo.project("PORTAIL");
        MvcResult stream = mvc.perform(get("/api/projects/" + portail + "/poker/stream")
                        .with(user("sarah@ezproject.local")).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mvc.perform(post("/api/projects/" + portail + "/poker/rounds").with(user("hugo@ezproject.local")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"storyTitle\":\"Story temps réel\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String roundId = body.replaceAll(".*?\"id\":(\\d+).*", "$1");
        mvc.perform(post("/api/poker/rounds/" + roundId + "/vote").with(user("hugo@ezproject.local")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"card\":\"13\"}")).andExpect(status().isOk());

        String events = stream.getResponse().getContentAsString();
        assertThat(events).contains("event:ready").contains("event:poker").contains("\"roundId\":" + roundId);
        assertThat(events).doesNotContain("\"card\"");
    }

    @Test
    void streamRequiresPokerAccess() throws Exception {
        mvc.perform(get("/api/projects/" + demo.project("PORTAIL") + "/poker/stream").with(user("nina@ezproject.local")))
                .andExpect(status().isNotFound());
    }
}
