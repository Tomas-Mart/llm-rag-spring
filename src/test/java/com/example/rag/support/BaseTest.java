package com.example.rag.support;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import com.example.rag.Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Базовый класс для модульных тестов.
 *
 * <p>Основные возможности:
 * <ul>
 *   <li>Загрузка Spring контекста с профилем {@code test}</li>
 *   <li>Моки для всех внешних зависимостей (Ollama, VectorStore, ChatClient, EmbeddingModel)</li>
 *   <li>Подключение к существующему контейнеру PostgreSQL</li>
 *   <li>Быстрое выполнение благодаря мокам</li>
 * </ul>
 *
 * <p>Важно: Для корректной загрузки контекста добавлен мок для {@link EmbeddingModel},
 * который требуется для создания {@link VectorStore}.
 *
 * @author RAG Application Team
 * @version 5.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {

    // ============================================================
    // ЛОГГЕР
    // ============================================================

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // ============================================================
    // SPRING BEANS
    // ============================================================

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected Environment environment;

    @Autowired
    protected Application application;

    // ============================================================
    // МОКИ ДЛЯ ВНЕШНИХ ЗАВИСИМОСТЕЙ
    // ============================================================

    @MockBean
    protected OllamaApi ollamaApi;

    @MockBean
    protected OllamaChatModel ollamaChatModel;

    @MockBean
    protected VectorStore vectorStore;

    @MockBean
    protected ChatClient chatClient;

    /**
     * Мок для EmbeddingModel.
     * Добавлен для удовлетворения зависимости VectorStore.
     */
    @MockBean
    protected EmbeddingModel embeddingModel;

    @Autowired(required = false)
    protected DataSource dataSource;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUpBase() {
        logger.info("🚀 Running test: {}", getTestName());
        logger.debug("📋 Test class: {}", getClass().getName());
        logger.debug("🐘 PostgreSQL URL: {}", environment.getProperty("spring.datasource.url"));
        logger.debug("📊 Active profiles: {}", String.join(", ", environment.getActiveProfiles()));

        assertAllBeansLoaded();
    }

    // ============================================================
    // ПРОВЕРКИ КОМПОНЕНТОВ
    // ============================================================

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

        assertThat(chatClient)
                .as("ChatClient mock should be created")
                .isNotNull();

        assertThat(embeddingModel)
                .as("EmbeddingModel mock should be created")
                .isNotNull();

        logger.info("✅ All mocks created successfully");
        logger.debug("   - OllamaApi: {}", ollamaApi.getClass().getSimpleName());
        logger.debug("   - OllamaChatModel: {}", ollamaChatModel.getClass().getSimpleName());
        logger.debug("   - VectorStore: {}", vectorStore.getClass().getSimpleName());
        logger.debug("   - ChatClient: {}", chatClient.getClass().getSimpleName());
        logger.debug("   - EmbeddingModel: {}", embeddingModel.getClass().getSimpleName());
    }

    protected void assertApplicationContextLoaded() {
        assertThat(application)
                .as("Application bean should be loaded")
                .isNotNull();

        assertThat(applicationContext)
                .as("ApplicationContext should be loaded")
                .isNotNull();

        logger.info("✅ Application context loaded successfully");
        logger.debug("   - Bean count: {}", applicationContext.getBeanDefinitionCount());
        logger.debug("   - Application name: {}", applicationContext.getApplicationName());
    }

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

            var metaData = connection.getMetaData();
            logger.info("✅ Database connection established successfully");
            logger.debug("   Database URL: {}", metaData.getURL());
            logger.debug("   Database Product: {}", metaData.getDatabaseProductName());
        } catch (SQLException e) {
            logger.error("❌ Failed to connect to database", e);
            throw e;
        }
    }

    protected void assertAllBeansLoaded() {
        assertApplicationContextLoaded();
        assertMocksCreated();
        logger.info("✅ All beans loaded successfully");
    }

    // ============================================================
    // УТИЛИТЫ
    // ============================================================

    protected String getTestName() {
        return getClass().getSimpleName();
    }

    protected void logTestStart(String message) {
        logger.info("🚀 [{}] {}", getTestName(), message);
    }

    protected void logTestSuccess(String message) {
        logger.info("✅ [{}] {}", getTestName(), message);
    }

    protected void logTestWarning(String message) {
        logger.warn("⚠️ [{}] {}", getTestName(), message);
    }

    protected void logTestError(String message, Throwable throwable) {
        logger.error("❌ [{}] {}", getTestName(), message, throwable);
    }
}