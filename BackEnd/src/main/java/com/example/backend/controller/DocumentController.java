package com.example.backend.controller;
import com.example.backend.model.DocumentResponse;
import com.example.backend.model.UploadResponse;
import com.example.backend.service.DocumentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/documents")
public class DocumentController {
 private final DocumentService documents; public DocumentController(DocumentService documents) { this.documents = documents; }
 @PostMapping("/upload") ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) { return ResponseEntity.status(HttpStatus.CREATED).body(documents.upload(file)); }
 @GetMapping List<DocumentResponse> list() { return documents.list(); }
 @DeleteMapping("/{fileName}") ResponseEntity<Void> delete(@PathVariable String fileName) { documents.delete(fileName); return ResponseEntity.noContent().build(); }
}
