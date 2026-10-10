package com.example.backend.model;
public record UploadResponse(boolean success, String message, String fileName, int chunksCreated) { }
