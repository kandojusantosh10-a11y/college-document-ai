package com.example.backend.model;
import java.util.List;
public record ChatResponse(String answer, List<SourceResponse> sources) { }
