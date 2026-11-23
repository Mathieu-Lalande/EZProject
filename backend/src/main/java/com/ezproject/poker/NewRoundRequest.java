package com.ezproject.poker;

import jakarta.validation.constraints.Size;

/** Soit une tâche existante (taskId), soit un titre libre. */
public record NewRoundRequest(Long taskId, @Size(max = 200) String storyTitle) {
}
