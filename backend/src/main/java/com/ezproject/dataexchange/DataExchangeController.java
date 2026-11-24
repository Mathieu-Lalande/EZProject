package com.ezproject.dataexchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;

@RestController
public class DataExchangeController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ImportService importService;
    private final ExportService exportService;

    public DataExchangeController(ImportService importService, ExportService exportService) {
        this.importService = importService;
        this.exportService = exportService;
    }

    @PostMapping(path = "/api/import/projects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportReport importProjects(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "true") boolean dryRun) throws IOException {
        return importService.importProjects(check(file).getInputStream(), dryRun);
    }

    @PostMapping(path = "/api/import/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportReport importTasks(@RequestParam("file") MultipartFile file,
                                    @RequestParam(defaultValue = "true") boolean dryRun) throws IOException {
        return importService.importTasks(check(file).getInputStream(), dryRun);
    }

    @GetMapping("/api/import/templates/{kind}.csv")
    public ResponseEntity<String> template(@PathVariable String kind) {
        return switch (kind) {
            case "projects" -> csv("modele-projets.csv", ExportService.PROJECT_TEMPLATE);
            case "tasks" -> csv("modele-taches.csv", ExportService.TASK_TEMPLATE);
            default -> throw new NotFoundException("Modèle", kind);
        };
    }

    @GetMapping("/api/export/portfolio.csv")
    public ResponseEntity<String> exportPortfolio(@RequestParam(required = false) Long teamId) {
        return csv("portefeuille.csv", exportService.portfolioCsv(teamId));
    }

    @GetMapping("/api/export/tasks.csv")
    public ResponseEntity<String> exportTasks(@RequestParam(required = false) Long teamId) {
        return csv("taches.csv", exportService.tasksCsv(teamId));
    }

    private static MultipartFile check(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("Fichier vide");
        }
        if (file.getSize() > 2L * 1024 * 1024) {
            throw new BusinessRuleException("Fichier trop volumineux (2 Mo maximum pour un import)");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".csv") && !name.endsWith(".txt")) {
            throw new BusinessRuleException("Format attendu : CSV (enregistrez votre Excel en « CSV UTF-8 »)");
        }
        return file;
    }

    private static ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
