package com.example.rag.support;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
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
 * Обеспечивает единую конфигурацию контейнеров для PostgreSQL и Ollama.
 *
 * <p>Основные возможности:
 * <ul>
 *   <li>Запуск PostgreSQL контейнера с pgvector</li>
 *   <li>Запуск Ollama контейнера</li>
 *   <li>Динамическая настройка свойств приложения</li>
 * </ul>
 *
 * <p>Пример использования:
 * <pre>{@code
 * @SpringBootTest
 * @Testcontainers
 * class MyIntegrationTest extends BaseIntegrationTestWithContainers {
 *
 *     @Test
 *     void testSomething() {
 *         // Используем контейнеры
 *     }
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("integration-test")
@Testcontainers
public abstract class BaseIntegrationTestWithContainers {

    /**
     * Логгер для всех тестовых классов.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Контейнер PostgreSQL с pgvector.
     * Используется для тестирования с реальной базой данных.
     *
     * <p>Используется {@link DockerImageName#asCompatibleSubstituteFor(String)}
     * для совместимости с Testcontainers PostgreSQL API.</p>
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
     * Мок для EmbeddingModel.
     * Используется для изоляции тестов от реального Ollama.
     *
     * <p>Этот мок предотвращает реальные вызовы к Ollama для создания эмбеддингов.
     * Возвращает фиктивный вектор для любого текста.</p>
     */
    @MockBean
    protected EmbeddingModel embeddingModel;

    /**
     * Контейнер Ollama.
     * Используется для тестирования с реальной LLM.
     *
     * <p>Используется {@code GenericContainer} для запуска Ollama.
     * Модель qwen2.5-coder:7b загружается при старте.</p>
     */
    @Container
    protected static final GenericContainer<?> OLLAMA_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("ollama/ollama:latest"))
                    .withExposedPorts(11434)
                    .withCommand("ollama pull qwen2.5-coder:7b")
                    .withReuse(true);

    /**
     * Динамическая настройка свойств приложения.
     * Подставляет URL контейнеров в конфигурацию Spring.
     *
     * @param registry реестр свойств для динамической настройки
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.show-sql", () -> "true");

        registry.add("spring.flyway.enabled", () -> "false");

        // Отключаем реальные вызовы к Ollama для эмбеддингов
        registry.add("spring.ai.ollama.base-url", () -> "http://localhost:11434");
        registry.add("spring.ai.ollama.embedding.enabled", () -> "false");
        registry.add("spring.ai.ollama.embedding.options.model", () -> "nomic-embed-text:v1.5");

        registry.add("spring.ai.vectorstore.pgvector.index-type", () -> "HNSW");
        registry.add("spring.ai.vectorstore.pgvector.distance-type", () -> "EUCLIDEAN_DISTANCE");
        registry.add("spring.ai.vectorstore.pgvector.dimensions", () -> "768");
        registry.add("spring.ai.vectorstore.pgvector.table-name", () -> "vector_store");
        registry.add("spring.ai.vectorstore.pgvector.drop-table", () -> "true");
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "true");
    }

    /**
     * Настройка мока перед каждым тестом.
     */
    @BeforeEach
    void setUpMock() {
        // Создаем фиктивный вектор размером 768
        float[] mockEmbedding = new float[768];
        for (int i = 0; i < 768; i++) {
            mockEmbedding[i] = (float) Math.random();
        }

        // Создаем Embedding объект с фиктивным вектором
        // Используем конструктор Embedding(float[] embedding, Integer index)
        Embedding mockEmbeddingObj = new Embedding(mockEmbedding, 0);

        // Создаем EmbeddingResponse с фиктивным вектором
        EmbeddingResponse mockResponse = new EmbeddingResponse(List.of(mockEmbeddingObj));

        // Настраиваем мок для метода embed (один текст)
        when(embeddingModel.embed(any(String.class))).thenReturn(mockEmbedding);

        // Настраиваем мок для метода embed (список текстов)
        when(embeddingModel.embed(any(List.class))).thenReturn(List.of(mockEmbedding));

        // Настраиваем мок для метода call
        when(embeddingModel.call(any())).thenReturn(mockResponse);

        logger.info("🔧 EmbeddingModel mock configured successfully");
        logger.debug("📊 Mock embedding vector size: {}", mockEmbedding.length);
    }

    /**
     * Проверяет, что PostgreSQL контейнер запущен и доступен.
     *
     * @return {@code true} если контейнер запущен, {@code false} в противном случае
     */
    protected boolean isPostgresRunning() {
        return POSTGRES_CONTAINER.isRunning();
    }

    /**
     * Проверяет, что Ollama контейнер запущен и доступен.
     *
     * @return {@code true} если контейнер запущен, {@code false} в противном случае
     */
    protected boolean isOllamaRunning() {
        return OLLAMA_CONTAINER.isRunning();
    }

    /**
     * Возвращает JDBC URL для подключения к PostgreSQL.
     *
     * @return JDBC URL контейнера
     */
    protected String getPostgresJdbcUrl() {
        return POSTGRES_CONTAINER.getJdbcUrl();
    }

    /**
     * Возвращает порт Ollama контейнера.
     *
     * @return порт Ollama
     */
    protected int getOllamaPort() {
        return OLLAMA_CONTAINER.getMappedPort(11434);
    }

    /**
     * Возвращает полный URL Ollama.
     *
     * @return URL Ollama
     */
    protected String getOllamaUrl() {
        return "http://localhost:" + getOllamaPort();
    }

    /**
     * Возвращает имя класса теста для использования в логировании.
     *
     * @return простое имя класса теста
     */
    protected String getTestName() {
        return getClass().getSimpleName();
    }
}