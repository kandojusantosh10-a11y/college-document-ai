package com.example.backend.service;

import com.example.backend.config.AppProperties;
import com.example.backend.exception.ApiException;
import com.example.backend.model.DocumentResponse;
import com.example.backend.model.UploadResponse;
import com.example.backend.util.SafeAiErrorLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class DocumentService {
    private final SimpleVectorStore vectorStore;
    private final AppProperties properties;
    private final ObjectMapper mapper;
    private final Map<String, StoredDocument> index = new LinkedHashMap<>();

    public DocumentService(SimpleVectorStore vectorStore, AppProperties properties, ObjectMapper mapper) {
        this.vectorStore = vectorStore; this.properties = properties; this.mapper = mapper;
        try { Files.createDirectories(Path.of(properties.getDocumentsDirectory())); Files.createDirectories(Path.of(properties.getVectorstoreDirectory())); loadIndex(); }
        catch (IOException e) { throw new IllegalStateException("Could not create application storage directories.", e); }
    }
    public synchronized UploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose a non-empty PDF file.");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("document.pdf").replaceAll("[^a-zA-Z0-9._ -]", "_");
        if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf") || !"application/pdf".equalsIgnoreCase(Optional.ofNullable(file.getContentType()).orElse("application/pdf")))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF files are supported.");
        if (index.containsKey(name)) delete(name);
        Path target = Path.of(properties.getDocumentsDirectory()).resolve(name).normalize();
        if (!target.startsWith(Path.of(properties.getDocumentsDirectory()).normalize())) throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file name.");
        try { Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the uploaded PDF."); }
        List<Document> chunks;
        try { chunks = readPdf(target, name); }
        catch (IOException e) { try { Files.deleteIfExists(target); } catch (IOException ignored) {} throw new ApiException(HttpStatus.BAD_REQUEST, "The uploaded file is not a readable PDF."); }
        if (chunks.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "No readable text was found in this PDF.");
        try { vectorStore.add(chunks); }
        catch (Exception e) {
            SafeAiErrorLogger.logFailure(org.slf4j.LoggerFactory.getLogger(DocumentService.class), "PDF embedding generation", e);
            try { Files.deleteIfExists(target); } catch (IOException ignored) {}
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to generate document embeddings. Check the Gemini API key, embedding model, and network connectivity.");
        }
        index.put(name, new StoredDocument(name, chunks.stream().map(Document::getId).toList(), chunks.size()));
        persist();
        return new UploadResponse(true, "Document processed successfully.", name, chunks.size());
    }
    public synchronized List<DocumentResponse> list() { return index.values().stream().map(d -> new DocumentResponse(d.fileName(), "PROCESSED", d.chunks().size())).toList(); }
    public synchronized boolean hasDocuments() { return !index.isEmpty(); }
    public synchronized void delete(String fileName) {
        StoredDocument stored = index.remove(fileName);
        if (stored == null) throw new ApiException(HttpStatus.NOT_FOUND, "Document not found.");
        vectorStore.delete(stored.chunks());
        try { Files.deleteIfExists(Path.of(properties.getDocumentsDirectory()).resolve(fileName)); } catch (IOException e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete the PDF file."); }
        persist();
    }
    private List<Document> readPdf(Path path, String fileName) throws IOException {
        List<Document> chunks = new ArrayList<>();
        try (PDDocument pdf = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page); stripper.setEndPage(page);
                String text = stripper.getText(pdf).replaceAll("\\s+", " ").trim();
                for (String chunk : split(text)) {
                    Map<String, Object> metadata = Map.of("fileName", fileName, "source", fileName, "page", page);
                    chunks.add(Document.builder().id(UUID.randomUUID().toString()).text(chunk).metadata(metadata).build());
                }
            }
        }
        return chunks;
    }
    private List<String> split(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + properties.getChunkSize());
            if (end < text.length()) { int boundary = Math.max(text.lastIndexOf(". ", end), text.lastIndexOf(" ", end)); if (boundary > start + 150) end = boundary + 1; }
            String chunk = text.substring(start, end).trim(); if (!chunk.isBlank()) result.add(chunk);
            if (end >= text.length()) break; start = Math.max(end - properties.getChunkOverlap(), start + 1);
        }
        return result;
    }
    private void loadIndex() throws IOException { File f = properties.documentIndexFile(); if (f.isFile()) index.putAll(mapper.readValue(f, new TypeReference<LinkedHashMap<String, StoredDocument>>() {})); }
    private void persist() { try { vectorStore.save(properties.vectorFile()); mapper.writerWithDefaultPrettyPrinter().writeValue(properties.documentIndexFile(), index); } catch (Exception e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not persist vector data."); } }
    public record StoredDocument(String fileName, List<String> chunks, int chunkCount) { }
}
