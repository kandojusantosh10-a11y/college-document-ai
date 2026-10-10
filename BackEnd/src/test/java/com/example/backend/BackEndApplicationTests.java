package com.example.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
class BackEndApplicationTests {
    @Test
    void contextLoadsWithConfiguredGeminiCredentials() { }
}