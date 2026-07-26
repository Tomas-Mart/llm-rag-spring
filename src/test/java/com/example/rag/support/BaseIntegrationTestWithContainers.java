package com.example.rag.support;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import com.example.rag.Application;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Application.class
)
@ActiveProfiles("integration-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@Tag("integration")
public abstract class BaseIntegrationTestWithContainers extends BaseIntegrationTest {

    private static final String EXISTING_POSTGRES_URL = "jdbc:postgresql://localhost:32769/rag_integration_test";
    private static final String EXISTING_POSTGRES_USER = "test_user";
    private static final String EXISTING_POSTGRES_PASSWORD = "test_password";
    private static final String EXISTING_OLLAMA_URL = "http://localhost:11434";
    private static final int EMBEDDING_DIMENSION = 768;

    static {
        log.info("========================================");
        log.info("🐘 PostgreSQL: {}", EXISTING_POSTGRES_URL);
        log.info("🦙 Ollama: {}", EXISTING_OLLAMA_URL);
        log.info("✅ Using existing containers for integration tests");
        log.info("========================================");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EXISTING_POSTGRES_URL);
        registry.add("spring.datasource.username", () -> EXISTING_POSTGRES_USER);
        registry.add("spring.datasource.password", () -> EXISTING_POSTGRES_PASSWORD);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.ai.ollama.base-url", () -> EXISTING_OLLAMA_URL);
        registry.add("spring.ai.ollama.chat.options.model", () -> "qwen2.5-coder:7b");
        registry.add("spring.ai.ollama.embedding.options.model", () -> "nomic-embed-text:v1.5");
        registry.add("spring.ai.vectorstore.pgvector.dimensions", () -> String.valueOf(EMBEDDING_DIMENSION));

        log.info("✅ Dynamic properties configured for existing containers");
    }

    @Configuration
    @Profile("integration-test")
    public static class MockConfig {

        @Bean
        @Primary
        public EmbeddingModel embeddingModel() {
            log.info("🔧 Creating test EmbeddingModel with random vectors (dimension: {})", EMBEDDING_DIMENSION);

            return new EmbeddingModel() {

                private float[] generateEmbedding(String text) {
                    float[] vector = new float[EMBEDDING_DIMENSION];
                    int hash = text != null ? text.hashCode() : 0;

                    for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                        vector[i] = (float) (Math.sin(hash + i * 0.1) * 0.5 + 0.5);
                    }
                    return vector;
                }

                @Override
                public @NotNull EmbeddingResponse call(@NotNull EmbeddingRequest request) {
                    List<String> texts = new ArrayList<>();

                    // Получаем инструкции из запроса
                    List<?> list = request.getInstructions();
                    for (Object item : list) {
                        if (item instanceof String) {
                            texts.add((String) item);
                        } else {
                            // Если это не строка, используем toString()
                            texts.add(item != null ? item.toString() : "");
                        }
                    }

                    // Если тексты пустые, добавляем пустую строку
                    if (texts.isEmpty()) {
                        texts.add("");
                    }

                    List<Embedding> embeddings = texts.stream()
                            .map(text -> new Embedding(generateEmbedding(text), 0))
                            .collect(Collectors.toList());

                    return new EmbeddingResponse(embeddings);
                }

                @Override
                public float @NotNull [] embed(@NotNull Document document) {
                    if (document.getText() == null || document.getText().trim().isEmpty()) {
                        return new float[EMBEDDING_DIMENSION];
                    }
                    return generateEmbedding(document.getText());
                }

                @Override
                public @NotNull List<float[]> embed(@NotNull List<String> texts) {
                    if (texts.isEmpty()) {
                        return new ArrayList<>();
                    }

                    return texts.stream()
                            .map(text -> text != null ? generateEmbedding(text) : new float[EMBEDDING_DIMENSION])
                            .collect(Collectors.toList());
                }

                @Override
                public int dimensions() {
                    return EMBEDDING_DIMENSION;
                }
            };
        }
    }
}