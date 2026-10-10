package com.example.backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
class GeminiConnectionIntegrationTest {
    @Autowired private ChatClient chatClient;
    @Autowired private EmbeddingModel embeddingModel;

    @Test
    void generatesAChatResponse() {
        String response = chatClient.prompt().user("Reply with the single word connected.").call().content();
        assertFalse(response == null || response.isBlank(), "Gemini chat returned an empty response");
    }

    @Test
    void generatesAnEmbedding() {
        float[] embedding = embeddingModel.embed("College regulations retrieval smoke test");
        assertTrue(embedding.length > 0, "Gemini embedding returned no vector");
    }
}