package com.example.rag.support;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.example.rag.Application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Абстрактный базовый класс для интеграционных тестов с использованием Testcontainers.
 * Профессиональная реализация с изоляцией внешних зависимостей.
 *
 * <p>Архитектурные решения:
 * <ul>
 *   <li>Использование @MockBean для изоляции от реальных LLM (Ollama)</li>
 *   <li>Настройка моков через @BeforeEach для гарантии чистоты состояния</li>
 *   <li>Динамическая конфигурация через @DynamicPropertySource</li>
 *   <li>Поддержка опционального запуска Ollama для реальных интеграционных тестов</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 2.0
 * @since 1.0
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("integration-test")
@Testcontainers
public abstract class BaseIntegrationTestWithContainers {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * PostgreSQL контейнер с pgvector.
     * Используется для тестирования с реальной БД.
     */
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:pg16")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("rag_db")
                    .withUsername("rag_user")
                    .withPassword("rag_pass")
                    .withInitScript("init-pgvector.sql");

    /**
     * Мок для EmbeddingModel - КЛЮЧЕВОЙ КОМПОНЕНТ!
     * Изолирует тесты от реального Ollama.
     */
    @MockBean
    protected EmbeddingModel embeddingModel;

    /**
     * Ollama контейнер - опциональный.
     * Запускается только если нужны реальные LLM тесты.
     */
    @Container
    protected static final GenericContainer<?> OLLAMA_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("ollama/ollama:latest"))
                    .withExposedPorts(11434)
                    .withCommand("serve")
                    .withReuse(true);

    /**
     * Динамическая конфигурация Spring.
     * Переопределяет свойства приложения для тестов.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.show-sql", () -> "false");

        // Flyway отключен
        registry.add("spring.flyway.enabled", () -> "false");

        // ============================================================
        // КЛЮЧЕВОЕ: ОТКЛЮЧАЕМ РЕАЛЬНЫЕ ЭМБЕДДИНГИ
        // ============================================================
        registry.add("spring.ai.ollama.base-url", () -> "http://localhost:11434");
        registry.add("spring.ai.ollama.embedding.enabled", () -> "false");
        registry.add("spring.ai.ollama.embedding.options.model", () -> "nomic-embed-text:v1.5");

        // Vector Store
        registry.add("spring.ai.vectorstore.pgvector.index-type", () -> "HNSW");
        registry.add("spring.ai.vectorstore.pgvector.distance-type", () -> "EUCLIDEAN_DISTANCE");
        registry.add("spring.ai.vectorstore.pgvector.dimensions", () -> "768");
        registry.add("spring.ai.vectorstore.pgvector.table-name", () -> "vector_store");
        registry.add("spring.ai.vectorstore.pgvector.drop-table", () -> "true");
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "true");
    }

    /**
     * Настройка мока перед каждым тестом.
     * Создает фиктивные эмбеддинги для изоляции от Ollama.
     */
    @BeforeEach
    void setUpMock() {
        // Генерируем детерминированный вектор для воспроизводимости
        float[] mockEmbedding = generateDeterministicEmbedding();

        // Создаем Embedding объект
        Embedding mockEmbeddingObj = new Embedding(mockEmbedding, 0);

        // Создаем EmbeddingResponse
        EmbeddingResponse mockResponse = new EmbeddingResponse(List.of(mockEmbeddingObj));

        // ============================================================
        // НАСТРОЙКА ВСЕХ МЕТОДОВ EmbeddingModel
        // ============================================================

        // 1. Для метода embed(String text) - возвращает float[]
        when(embeddingModel.embed(any(String.class))).thenReturn(mockEmbedding);

        // 2. Для метода embed(List<String> texts) - возвращает List<float[]>
        when(embeddingModel.embed(any(List.class))).thenReturn(List.of(mockEmbedding));

        // 3. Для метода call(EmbeddingRequest request) - возвращает EmbeddingResponse
        when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(mockResponse);

        logger.info("🔧 EmbeddingModel mock configured with deterministic vector");
        logger.debug("📊 Vector dimension: {}", mockEmbedding.length);
        logger.debug("📊 Mock response contains {} embeddings", mockResponse.getResults().size());
    }

    /**
     * Генерирует детерминированный вектор для воспроизводимости тестов.
     * Использует фиксированное seed для гарантии одинаковых результатов.
     */
    private float[] generateDeterministicEmbedding() {
        float[] embedding = new float[768];
        for (int i = 0; i < embedding.length; i++) {
            // Детерминированное значение на основе индекса
            embedding[i] = (float) (Math.sin(i) * 0.5 + 0.5);
        }
        return embedding;
    }

    /**
     * Проверяет запуск PostgreSQL контейнера.
     */
    protected boolean isPostgresRunning() {
        return POSTGRES_CONTAINER.isRunning();
    }

    /**
     * Проверяет запуск Ollama контейнера.
     */
    protected boolean isOllamaRunning() {
        return OLLAMA_CONTAINER.isRunning();
    }

    /**
     * Возвращает JDBC URL.
     */
    protected String getPostgresJdbcUrl() {
        return POSTGRES_CONTAINER.getJdbcUrl();
    }

    /**
     * Возвращает порт Ollama.
     */
    protected int getOllamaPort() {
        return OLLAMA_CONTAINER.getMappedPort(11434);
    }

    /**
     * Возвращает URL Ollama.
     */
    protected String getOllamaUrl() {
        return "http://localhost:" + getOllamaPort();
    }

    /**
     * Возвращает имя теста для логирования.
     */
    protected String getTestName() {
        return getClass().getSimpleName();
    }
}