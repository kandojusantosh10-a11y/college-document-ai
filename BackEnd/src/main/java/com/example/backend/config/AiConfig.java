package com.example.backend.config;

import java.io.File;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    SimpleVectorStore vectorStore(EmbeddingModel embeddingModel, AppProperties properties) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File savedStore = properties.vectorFile();
        if (savedStore.isFile() && savedStore.length() > 0) {
            store.load(savedStore);
        }
        return store;
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
