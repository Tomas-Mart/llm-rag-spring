package com.example.rag.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.example.rag.Application;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.Mockito.mock;

/**
 * Базовый класс для ИНТЕГРАЦИОННЫХ тестов с Testcontainers.
 * <p>
 * Особенности:
 * <ul>
 *   <li>Автоматический запуск PostgreSQL + pgvector в контейнере</li>
 *   <li>Не требует внешней базы данных</li>
 *   <li>Мок для {@link EmbeddingModel} с {@code @Primary}</li>
 *   <li>Изоляция от реальных LLM вызовов</li>
 * </ul>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * @Slf4j
 * class ControllerIT extends BaseIntegrationTestWithContainers {
 *     @Autowired
 *     private MockMvc mockMvc;
 *
 *     @Test
 *     void testEndpoints() throws Exception {
 *         mockMvc.perform(get("/api/health")).andExpect(status().isOk());
 *     }
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Application.class
)
@ActiveProfiles("integration-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@Tag("integration")
public abstract class BaseIntegrationTestWithContainers extends BaseIntegrationTest {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String POSTGRES_IMAGE = "pgvector/pgvector:pg16";
    private static final String OLLAMA_IMAGE = "ollama/ollama:latest";
    private static final String DATABASE_NAME = "rag_test";
    private static final String DATABASE_USER = "test_user";
    private static final String DATABASE_PASSWORD = "test_password";
    private static final int OLLAMA_PORT = 11434;

    // ============================================================
    // КОНТЕЙНЕРЫ
    // ============================================================

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                    .withDatabaseName(DATABASE_NAME)
                    .withUsername(DATABASE_USER)
                    .withPassword(DATABASE_PASSWORD)
                    .withReuse(true);

    @Container
    protected static final GenericContainer<?> OLLAMA_CONTAINER =
            new GenericContainer<>(DockerImageName.parse(OLLAMA_IMAGE))
                    .withExposedPorts(OLLAMA_PORT)
                    .withCommand("serve")
                    .withReuse(true);

    // ============================================================
    // ДИНАМИЧЕСКАЯ КОНФИГУРАЦИЯ
    // ============================================================

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        log.info("🐘 Testcontainers PostgreSQL started on: {}", POSTGRES_CONTAINER.getJdbcUrl());
    }

    // ============================================================
    // МЕТОДЫ ПРОВЕРКИ СОСТОЯНИЯ КОНТЕЙНЕРОВ
    // ============================================================

    protected boolean isPostgresRunning() {
        return POSTGRES_CONTAINER != null && POSTGRES_CONTAINER.isRunning();
    }

    protected boolean isOllamaRunning() {
        return OLLAMA_CONTAINER != null && OLLAMA_CONTAINER.isRunning();
    }

    protected int getOllamaPort() {
        if (!isOllamaRunning()) {
            throw new IllegalStateException("Ollama container is not running");
        }
        return OLLAMA_CONTAINER.getMappedPort(OLLAMA_PORT);
    }

    protected String getPostgresJdbcUrl() {
        if (!isPostgresRunning()) {
            throw new IllegalStateException("PostgreSQL container is not running");
        }
        return POSTGRES_CONTAINER.getJdbcUrl();
    }

    protected String getPostgresHost() {
        if (!isPostgresRunning()) {
            throw new IllegalStateException("PostgreSQL container is not running");
        }
        return POSTGRES_CONTAINER.getHost();
    }

    protected int getPostgresPort() {
        if (!isPostgresRunning()) {
            throw new IllegalStateException("PostgreSQL container is not running");
        }
        return POSTGRES_CONTAINER.getMappedPort(5432);
    }

    // ============================================================
    // КОНФИГУРАЦИЯ МОКОВ
    // ============================================================

    @Configuration
    @Profile("integration-test")
    public static class MockConfig {

        @Bean
        @Primary
        public EmbeddingModel embeddingModel() {
            log.info("🔧 Creating mock EmbeddingModel for integration tests");
            return mock(EmbeddingModel.class);
        }
    }
}