package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class ChatServiceRetrievalIsolationTest {
    @Test
    void staleFitlifeChunksAreExcludedFromContextCandidatesAndCitations() {
        ChatService chat = new ChatService(null, null, null, null);
        Document sample = Document.builder().id("sample-chunk-1").text("Attendance requirements and college policies.")
                .metadata(Map.of("fileName", "College_Regulations_Sample.pdf", "page", 1)).build();
        Document stale = Document.builder().id("stale-fitlife-1").text("FITLIFE report content.")
                .metadata(Map.of("fileName", "FITLIFE_AI_PULSE_ENHANCED_FINAL_REPORT.pdf", "page", 3)).build();
        Document malformed = Document.builder().id("sample-chunk-bad-metadata").text("Uncitable content.")
                .metadata(Map.of("fileName", "College_Regulations_Sample.pdf", "page", "unknown")).build();

        List<Document> eligible = chat.filterIndexedChunks(List.of(sample, stale, malformed), Set.of("sample-chunk-1"));

        assertEquals(List.of("sample-chunk-1"), eligible.stream().map(Document::getId).toList());
        assertEquals("College_Regulations_Sample.pdf", chat.sources(eligible).get(0).fileName());
        assertTrue(chat.sources(eligible).stream().noneMatch(source -> source.fileName().contains("FITLIFE")));
    }
}
