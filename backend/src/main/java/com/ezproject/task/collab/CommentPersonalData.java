package com.ezproject.task.collab;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.ezproject.account.PersonalDataContributor;
import com.ezproject.user.AppUser;

/** RGPD : commentaires inclus dans l'export ; conservés à l'effacement, attribués au compte anonymisé. */
@Component
class CommentPersonalData implements PersonalDataContributor {

    private final TaskCommentRepository comments;

    CommentPersonalData(TaskCommentRepository comments) {
        this.comments = comments;
    }

    @Override
    public String key() {
        return "taskComments";
    }

    @Override
    public Object export(AppUser user) {
        return comments.findByAuthorId(user.getId()).stream()
                .map(c -> Map.of("task", c.getTask().getTitle(), "body", c.getBody(), "createdAt", c.getCreatedAt().toString()))
                .toList();
    }

    @Override
    public void erase(AppUser user) {
        // Contenu de travail conservé ; l'auteur est anonymisé avec le compte
    }
}
