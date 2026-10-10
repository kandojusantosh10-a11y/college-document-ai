package com.example.backend.model;

import jakarta.validation.constraints.NotBlank;
public record ChatRequest(@NotBlank(message = "Question cannot be empty.") String question) { }
