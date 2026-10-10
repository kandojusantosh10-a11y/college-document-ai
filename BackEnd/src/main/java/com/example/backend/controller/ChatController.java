package com.example.backend.controller;
import com.example.backend.model.ChatRequest;
import com.example.backend.model.ChatResponse;
import com.example.backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/chat")
public class ChatController { private final ChatService chat; public ChatController(ChatService chat) { this.chat = chat; } @PostMapping ChatResponse ask(@Valid @RequestBody ChatRequest request) { return chat.answer(request.question()); } }
