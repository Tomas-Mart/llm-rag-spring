package com.example.rag.support;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import com.example.rag.Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Базовый класс для модульных тестов с использованием реальной PostgreSQL.
 * В отличие от интеграционных тестов, здесь используется in-memory PostgreSQL
 * через Testcontainers для быстрых тестов.
 *
 * <p>Основные возможности:
 * <ul>
 *   <li>Загрузка Spring контекста с профилем {@code test}</li>
 *   <li>Использование Testcontainers для PostgreSQL с pgvector</li>
 *   <li>Моки для всех внешних зависимостей (Ollama, VectorStore)</li>
 *   <li>Быстрое выполнение за счет in-memory режима</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 3.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {

    /**
     * Логгер для всех тестовых классов.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Контейнер PostgreSQL с pgvector для модульных тестов.
     * Используется in-memory режим для ускорения.
     */
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>(
                DockerImageName.parse("pgvector/pgvector:pg16")
                        .asCompatibleSubstituteFor("postgres")
        )
                .withDatabaseName("rag_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withReuse(true)
                .withCommand("postgres", "-c", "shared_buffers=256MB", "-c", "max_connections=20");

        POSTGRES_CONTAINER.start();

        Logger logger = LoggerFactory.getLogger(BaseTest.class);
        logger.info("🐘 Test PostgreSQL container started at: {}", POSTGRES_CONTAINER.getJdbcUrl());
    }

    /**
     * Динамические свойства для подключения к PostgreSQL.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
    }

    /**
     * Контекст приложения.
     */
    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * Environment для доступа к свойствам конфигурации.
     * Добавлен для использования в тестах.
     */
    @Autowired
    protected Environment environment;  // ← ДОБАВИТЬ

    /**
     * Главный Application bean.
     */
    @Autowired
    protected Application application;

    /**
     * Мок для Ollama API.
     */
    @MockBean
    protected OllamaApi ollamaApi;

    /**
     * Мок для Ollama Chat Model.
     */
    @MockBean
    protected OllamaChatModel ollamaChatModel;

    /**
     * Мок для VectorStore (в модульных тестах используем мок).
     */
    @MockBean
    protected VectorStore vectorStore;

    /**
     * Реальный DataSource для подключения к PostgreSQL.
     */
    @Autowired(required = false)
    protected DataSource dataSource;

    /**
     * Инициализация перед каждым тестом.
     */
    @BeforeEach
    void setUpBase() {
        logger.info("🚀 Running test: {}", getTestName());
        logger.debug("📋 Test class: {}", getClass().getName());
        logger.debug("🐘 PostgreSQL URL: {}", POSTGRES_CONTAINER.getJdbcUrl());
        logger.debug("📊 Active profiles: {}",
                String.join(", ", environment.getActiveProfiles()));  // ← ТЕПЕРЬ РАБОТАЕТ

        assertAllBeansLoaded();
    }

    /**
     * Проверяет, что все моки созданы.
     */
    protected void assertMocksCreated() {
        assertThat(ollamaApi)
                .as("OllamaApi mock should be created")
                .isNotNull();

        assertThat(ollamaChatModel)
                .as("OllamaChatModel mock should be created")
                .isNotNull();

        assertThat(vectorStore)
                .as("VectorStore mock should be created")
                .isNotNull();

        logger.info("✅ All mocks created successfully");
    }

    /**
     * Проверяет, что контекст загружен.
     */
    protected void assertApplicationContextLoaded() {
        assertThat(application)
                .as("Application bean should be loaded")
                .isNotNull();

        assertThat(applicationContext)
                .as("ApplicationContext should be loaded")
                .isNotNull();

        logger.info("✅ Application context loaded successfully");
        logger.debug("   - Bean count: {}", applicationContext.getBeanDefinitionCount());
    }

    /**
     * Проверяет подключение к базе данных.
     */
    protected void assertDataSourceAvailable() throws SQLException {
        if (dataSource == null) {
            logger.warn("⚠️ DataSource is not available");
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection)
                    .as("Database connection should be established")
                    .isNotNull();
            assertThat(connection.isValid(5))
                    .as("Connection should be valid")
                    .isTrue();

            logger.info("✅ Database connection established successfully");
        } catch (SQLException e) {
            logger.error("❌ Failed to connect to database", e);
            throw e;
        }
    }

    /**
     * Проверяет все компоненты.
     */
    protected void assertAllBeansLoaded() {
        assertApplicationContextLoaded();
        assertMocksCreated();
        logger.info("✅ All beans loaded successfully");
    }

    /**
     * Возвращает имя класса теста.
     */
    protected String getTestName() {
        return getClass().getSimpleName();
    }

    /**
     * Логирует начало выполнения теста.
     */
    protected void logTestStart(String message) {
        logger.info("🚀 [{}] {}", getTestName(), message);
    }

    /**
     * Логирует успешное завершение теста.
     */
    protected void logTestSuccess(String message) {
        logger.info("✅ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует предупреждение.
     */
    protected void logTestWarning(String message) {
        logger.warn("⚠️ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует ошибку.
     */
    protected void logTestError(String message, Throwable throwable) {
        logger.error("❌ [{}] {}", getTestName(), message, throwable);
    }
}