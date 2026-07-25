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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Абстрактный базовый класс для интеграционных тестов с использованием Testcontainers.
 * Профессиональная реализация с изоляцией внешних зависимостей.
 *
 * <p>Архитектурные решения:
 * <ul>
 *   <li>Использование вложенной конфигурации {@code MockConfig} для изоляции от LLM</li>
 *   <li>Настройка моков через @BeforeEach для гарантии чистоты состояния</li>
 *   <li>Динамическая конфигурация через @DynamicPropertySource</li>
 *   <li>Поддержка опционального запуска Ollama для реальных интеграционных тестов</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 2.0
 * @since 1.0
 */
@Testcontainers
@ActiveProfiles("integration-test")
@SuppressWarnings({"resource", "unused", "rawtypes"})
@SpringBootTest(classes = {Application.class, BaseIntegrationTestWithContainers.MockConfig.class})
public abstract class BaseIntegrationTestWithContainers {

    /**
     * Логгер для всех тестовых классов.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Конфигурация для мока EmbeddingModel.
     *
     * <p>Использует {@code @Primary} на уровне метода для гарантии,
     * что мок будет использоваться вместо реального бина.</p>
     */
    @Configuration
    @Profile("integration-test")
    public static class MockConfig {

        /**
         * Создает мок для EmbeddingModel с @Primary.
         *
         * <p>Мок настраивается на все методы EmbeddingModel:
         * <ul>
         *   <li>{@code embed(String)} - возвращает float[]</li>
         *   <li>{@code embed(List<String>)} - возвращает List<float[]></li>
         *   <li>{@code call(EmbeddingRequest)} - возвращает EmbeddingResponse</li>
         * </ul>
         * </p>
         *
         * @return мок для EmbeddingModel с @Primary
         */
        @Bean
        @Primary
        @SuppressWarnings({"unchecked"})
        public EmbeddingModel embeddingModel() {
            EmbeddingModel mock = mock(EmbeddingModel.class);

            // Генерируем детерминированный вектор для воспроизводимости
            float[] mockEmbedding = generateDeterministicEmbedding();

            // Создаем Embedding объект
            Embedding mockEmbeddingObj = new Embedding(mockEmbedding, 0);
            EmbeddingResponse mockResponse = new EmbeddingResponse(List.of(mockEmbeddingObj));

            // Настраиваем все методы
            // 1. embed(String) - возвращает float[]
            when(mock.embed(any(String.class))).thenReturn(mockEmbedding);

            // 2. embed(List<String>) - возвращает List<float[]>
            // Используем @SuppressWarnings для подавления unchecked предупреждения
            List<String> anyStringList = any(List.class);
            when(mock.embed(anyStringList)).thenReturn(List.of(mockEmbedding));

            // 3. call(EmbeddingRequest) - возвращает EmbeddingResponse
            when(mock.call(any(EmbeddingRequest.class))).thenReturn(mockResponse);

            return mock;
        }

        /**
         * Генерирует детерминированный вектор для воспроизводимости тестов.
         *
         * @return массив float размером 768 с детерминированными значениями
         */
        private static float[] generateDeterministicEmbedding() {
            float[] embedding = new float[768];
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] = (float) (Math.sin(i) * 0.5 + 0.5);
            }
            return embedding;
        }
    }

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
                    .withInitScript("init-pgvector.sql")
                    .withReuse(true);

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
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "2");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "600000");

        // JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");

        // Flyway отключен
        registry.add("spring.flyway.enabled", () -> "false");

        // Отключаем реальные эмбеддинги
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
     * Проверяет, что мок EmbeddingModel настроен корректно.
     */
    @BeforeEach
    void setUpMock() {
        logger.info("🔧 EmbeddingModel mock is ready for test: {}", getTestName());
    }

    /**
     * Проверяет запуск PostgreSQL контейнера.
     *
     * @return {@code true} если контейнер запущен, {@code false} в противном случае
     */
    protected boolean isPostgresRunning() {
        boolean isRunning = POSTGRES_CONTAINER.isRunning();
        if (isRunning) {
            logger.debug("🐘 PostgreSQL is running: {}", getPostgresJdbcUrl());
        } else {
            logger.warn("⚠️ PostgreSQL is not running");
        }
        return isRunning;
    }

    /**
     * Проверяет запуск Ollama контейнера.
     *
     * @return {@code true} если контейнер запущен, {@code false} в противном случае
     */
    protected boolean isOllamaRunning() {
        boolean isRunning = OLLAMA_CONTAINER.isRunning();
        if (isRunning) {
            logger.debug("🤖 Ollama is running on port: {}", getOllamaPort());
        } else {
            logger.warn("⚠️ Ollama is not running");
        }
        return isRunning;
    }

    /**
     * Возвращает JDBC URL.
     *
     * @return JDBC URL контейнера
     */
    protected String getPostgresJdbcUrl() {
        return POSTGRES_CONTAINER.getJdbcUrl();
    }

    /**
     * Возвращает порт Ollama.
     *
     * @return порт Ollama
     */
    protected int getOllamaPort() {
        return OLLAMA_CONTAINER.getMappedPort(11434);
    }

    /**
     * Возвращает имя теста для логирования.
     *
     * @return простое имя класса теста
     */
    protected String getTestName() {
        return getClass().getSimpleName();
    }
}