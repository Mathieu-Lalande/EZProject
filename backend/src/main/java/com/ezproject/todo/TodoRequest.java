package com.ezproject.todo;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TodoRequest(@NotBlank @Size(max = 300) String title, boolean done, LocalDate dueDate) {
}
