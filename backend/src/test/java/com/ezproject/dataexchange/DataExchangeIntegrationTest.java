package com.ezproject.dataexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.project.ProjectRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class DataExchangeIntegrationTest {

    private static final String PROJECTS = """
            code;nom;type;statut;motif_statut;priorite;météo;équipe;échéance
            CRM;Refonte CRM;Développement;En cours;;P2;Vert;Nouvelle équipe;30/06/2027
            PORTAIL;Portail client;;;;P1;Rouge;;
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ProjectRepository projects;

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "import.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    /** Hors transaction de test : on vérifie que l'import a réellement été annulé en base. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dryRunReportsWithoutPersisting() throws Exception {
        mvc.perform(multipart("/api/import/projects").file(csv(PROJECTS)).param("dryRun", "true")
                        .with(user("lea@ezproject.local")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.errors.length()").value(0));
        assertThat(projects.findAll()).noneMatch(p -> p.getCode().equals("CRM"));
    }

    @Test
    void importUpsertsProjectsAndCreatesMissingTeams() throws Exception {
        mvc.perform(multipart("/api/import/projects").file(csv(PROJECTS)).param("dryRun", "false")
                        .with(user("lea@ezproject.local")).with(csrf()))
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.teamsCreated").value(1));
        var crm = projects.findAll().stream().filter(p -> p.getCode().equals("CRM")).findFirst().orElseThrow();
        assertThat(crm.getTeam().getName()).isEqualTo("Nouvelle équipe");
        assertThat(crm.getEndDate()).hasToString("2027-06-30");
        var portail = projects.findAll().stream().filter(p -> p.getCode().equals("PORTAIL")).findFirst().orElseThrow();
        assertThat(portail.getHealth().name()).isEqualTo("RED");
        assertThat(portail.getClient()).isEqualTo("Banque Horizon"); // colonne vide : valeur conservée
    }

    /** Hors transaction de test : on vérifie que l'import a réellement été annulé en base. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void anyErrorCancelsTheWholeImport() throws Exception {
        String bad = """
                code,nom,statut
                OK1,Projet valide,En cours
                KO1,Projet en pause sans motif,En pause
                KO2,,Inconnu
                """;
        mvc.perform(multipart("/api/import/projects").file(csv(bad)).param("dryRun", "false")
                        .with(user("lea@ezproject.local")).with(csrf()))
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].line").value(3));
        assertThat(projects.findAll()).noneMatch(p -> p.getCode().equals("OK1"));
    }

    @Test
    void membersCannotImportProjects() throws Exception {
        mvc.perform(multipart("/api/import/projects").file(csv(PROJECTS)).with(user("sarah@ezproject.local")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void tasksImportChecksProjectPermissions() throws Exception {
        String tasks = """
                projet;titre;statut;assigné;points
                PORTAIL;Tâche importée;À faire;sarah@ezproject.local;3
                AUDITSI;Hors de mon équipe;À faire;;
                """;
        mvc.perform(multipart("/api/import/tasks").file(csv(tasks)).param("dryRun", "false")
                        .with(user("hugo@ezproject.local")).with(csrf()))
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.errors[0].line").value(3));
    }

    @Test
    void exportIsProtectedAgainstCsvInjectionAndScoped() throws Exception {
        String body = mvc.perform(get("/api/export/portfolio.csv").with(user("nina@ezproject.local")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).startsWith("﻿code;nom").contains("AUDITSI").doesNotContain("PORTAIL").doesNotContain("MANDA");
        assertThat(CsvExport.cell("=HYPERLINK(\"x\")")).startsWith("\"'=");
        assertThat(CsvExport.cell("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(CsvExport.cell(-5)).isEqualTo("-5");
    }
}
