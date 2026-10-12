package com.example.backend.service;

import com.example.backend.config.AppProperties;
import com.example.backend.exception.ApiException;
import com.example.backend.model.ChatResponse;
import com.example.backend.model.SourceResponse;
import com.example.backend.util.SafeAiErrorLogger;
import java.util.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private static final String NOT_FOUND = "I couldn't find this information in the uploaded college documents.";
    private final SimpleVectorStore vectorStore; private final ChatClient chatClient; private final AppProperties properties; private final DocumentService documents;
    public ChatService(SimpleVectorStore vectorStore, ChatClient chatClient, AppProperties properties, DocumentService documents) { this.vectorStore = vectorStore; this.chatClient = chatClient; this.properties = properties; this.documents = documents; }
    public ChatResponse answer(String question) {
        Set<String> indexedFileNames = documents.indexedFileNames();
        Set<String> indexedChunkIds = documents.indexedChunkIds();
        if (indexedFileNames.isEmpty() || indexedChunkIds.isEmpty()) return new ChatResponse(NOT_FOUND, List.of());
        List<Document> matches;
        try {
            var fileNameFilter = new FilterExpressionBuilder().in("fileName", indexedFileNames.toArray()).build();
            matches = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(properties.getRagTopK()).similarityThreshold(properties.getRagSimilarityThreshold()).filterExpression(fileNameFilter).build());
            matches = filterIndexedChunks(matches, indexedChunkIds);
        }
        catch (Exception e) { SafeAiErrorLogger.logFailure(org.slf4j.LoggerFactory.getLogger(ChatService.class), "Query embedding and vector search", e); throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to search documents. Check the Gemini API key, embedding model, and network connectivity."); }
        if (matches == null || matches.isEmpty()) return new ChatResponse(NOT_FOUND, List.of());
        String context = matches.stream().map(d -> "[Source: " + d.getMetadata().get("fileName") + ", page " + d.getMetadata().get("page") + "]\n" + d.getText()).reduce("", (a, b) -> a + "\n\n" + b);
        String prompt = "You are a college document assistant. Answer using ONLY the document context below. If it does not contain enough information, reply exactly: '" + NOT_FOUND + "'. Do not use outside knowledge or invent facts. Keep the answer clear and concise.\n\nDOCUMENT CONTEXT:\n" + context + "\n\nQUESTION: " + question;
        try {
            String answer = chatClient.prompt().user(prompt).call().content();
            return new ChatResponse(answer == null || answer.isBlank() ? NOT_FOUND : answer.trim(), sources(matches));
        } catch (Exception e) { SafeAiErrorLogger.logFailure(org.slf4j.LoggerFactory.getLogger(ChatService.class), "Gemini answer generation", e); throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to generate an answer. Check the Gemini API key, chat model, and network connectivity."); }
    }
    List<Document> filterIndexedChunks(List<Document> docs, Set<String> indexedChunkIds) {
        if (docs == null || indexedChunkIds == null || indexedChunkIds.isEmpty()) return List.of();
        return docs.stream().filter(Objects::nonNull)
                .filter(d -> d.getId() != null && indexedChunkIds.contains(d.getId()))
                .filter(d -> d.getMetadata() != null
                        && d.getMetadata().get("fileName") instanceof String fileName && !fileName.isBlank()
                        && d.getMetadata().get("page") instanceof Number page && page.intValue() > 0)
                .toList();
    }
    List<SourceResponse> sources(List<Document> docs) {
        Set<String> seen = new LinkedHashSet<>(); List<SourceResponse> result = new ArrayList<>();
        for (Document d : docs) {
            if (d == null || d.getMetadata() == null
                    || !(d.getMetadata().get("fileName") instanceof String name) || name.isBlank()
                    || !(d.getMetadata().get("page") instanceof Number page) || page.intValue() <= 0) continue;
            if (seen.add(name + page.intValue())) result.add(new SourceResponse(name, page.intValue()));
        }
        return result;
    }
}
