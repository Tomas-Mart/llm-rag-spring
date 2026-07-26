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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.example.rag.Application;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Testcontainers(disabledWithoutDocker = true)  // ← Добавлено
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Application.class
)
@ActiveProfiles("integration-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@Tag("integration")
public abstract class BaseIntegrationTestWithContainers extends BaseIntegrationTest {

    private static final int EMBEDDING_DIMENSION = 768;

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"))
                    .withDatabaseName("rag_integration_test")
                    .withUsername("test_user")
                    .withPassword("test_password")
                    .withReuse(true);

    static {
        log.info("========================================");
        log.info("🐘 PostgreSQL Testcontainer starting...");
        POSTGRES_CONTAINER.start();
        log.info("✅ PostgreSQL container started");
        log.info("   📍 JDBC URL: {}", POSTGRES_CONTAINER.getJdbcUrl());
        log.info("   🔌 Host: {}", POSTGRES_CONTAINER.getHost());
        log.info("   🔌 Port: {}", POSTGRES_CONTAINER.getMappedPort(5432));
        log.info("========================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (POSTGRES_CONTAINER.isRunning()) {
                POSTGRES_CONTAINER.stop();
                log.info("🐘 PostgreSQL container stopped");
            }
        }));
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.ai.vectorstore.pgvector.dimensions", () -> String.valueOf(EMBEDDING_DIMENSION));
        registry.add("spring.ai.ollama.base-url", () ->
                System.getenv("OLLAMA_URL") != null ? System.getenv("OLLAMA_URL") : "http://localhost:11434"
        );
        registry.add("spring.ai.ollama.chat.options.model", () -> "qwen2.5-coder:7b");
        registry.add("spring.ai.ollama.embedding.options.model", () -> "nomic-embed-text:v1.5");

        log.info("✅ Dynamic properties configured");
    }

    @Configuration
    @Profile("integration-test")
    public static class MockConfig {

        @Bean
        @Primary
        public EmbeddingModel embeddingModel() {
            log.info("🔧 Creating test EmbeddingModel (dimension: {})", EMBEDDING_DIMENSION);

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
                    List<?> list = request.getInstructions();
                    for (Object item : list) {
                        texts.add(item != null ? item.toString() : "");
                    }
                    if (texts.isEmpty()) texts.add("");

                    List<Embedding> embeddings = texts.stream()
                            .map(text -> new Embedding(generateEmbedding(text), 0))
                            .collect(Collectors.toList());
                    return new EmbeddingResponse(embeddings);
                }

                @Override
                public float @NotNull [] embed(@NotNull Document document) {
                    return generateEmbedding(document.getText());
                }

                @Override
                public @NotNull List<float[]> embed(@NotNull List<String> texts) {
                    return texts.stream()
                            .map(this::generateEmbedding)
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