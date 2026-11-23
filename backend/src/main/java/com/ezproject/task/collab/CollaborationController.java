package com.ezproject.task.collab;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ezproject.task.collab.CollaborationDtos.AttachmentDto;
import com.ezproject.task.collab.CollaborationDtos.ChecklistDto;
import com.ezproject.task.collab.CollaborationDtos.ChecklistRequest;
import com.ezproject.task.collab.CollaborationDtos.CommentDto;
import com.ezproject.task.collab.CollaborationDtos.CommentRequest;
import com.ezproject.task.collab.CollaborationDtos.TaskDetailsDto;

import jakarta.validation.Valid;

@RestController
public class CollaborationController {

    private final CollaborationService service;

    public CollaborationController(CollaborationService service) {
        this.service = service;
    }

    @GetMapping("/api/tasks/{taskId}/details")
    public TaskDetailsDto details(@PathVariable Long taskId) {
        return service.details(taskId);
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto comment(@PathVariable Long taskId, @Valid @RequestBody CommentRequest request) {
        return service.comment(taskId, request.body());
    }

    @PutMapping("/api/comments/{id}")
    public CommentDto editComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        return service.editComment(id, request.body());
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id) {
        service.deleteComment(id);
    }

    @PostMapping("/api/tasks/{taskId}/checklist")
    public List<ChecklistDto> addItem(@PathVariable Long taskId, @Valid @RequestBody ChecklistRequest request) {
        return service.addItem(taskId, request.title());
    }

    @PutMapping("/api/checklist/{id}")
    public List<ChecklistDto> updateItem(@PathVariable Long id, @Valid @RequestBody ChecklistRequest request) {
        return service.updateItem(id, request.title(), request.done());
    }

    @DeleteMapping("/api/checklist/{id}")
    public List<ChecklistDto> deleteItem(@PathVariable Long id) {
        return service.deleteItem(id);
    }

    @PostMapping(path = "/api/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentDto attach(@PathVariable Long taskId, @RequestParam("file") MultipartFile file) throws IOException {
        return service.attach(taskId, file.getOriginalFilename(), file.getBytes());
    }

    /**
     * Téléchargement : toujours en pièce jointe (jamais affiché dans le contexte de l'application),
     * avec le type MIME de la liste blanche ; X-Content-Type-Options: nosniff est ajouté par Spring Security.
     */
    @GetMapping("/api/attachments/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        CollaborationService.Download d = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.filename(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("Content-Security-Policy", "sandbox; default-src 'none'")
                .body(d.content());
    }

    @DeleteMapping("/api/attachments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@PathVariable Long id) {
        service.deleteAttachment(id);
    }
}
