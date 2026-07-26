package com.example.rag.support;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
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
import org.springframework.test.context.TestPropertySource;
import com.example.rag.Application;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Базовый класс для МОДУЛЬНЫХ тестов.
 * <p>
 * Особенности:
 * <ul>
 *   <li>Использует {@code @SpringBootTest} с MOCK окружением</li>
 *   <li>Все внешние зависимости заменены на моки</li>
 *   <li>Использует H2 in-memory базу данных для быстрых тестов</li>
 *   <li>AI компоненты ОТКЛЮЧЕНЫ</li>
 *   <li><b>ИСКЛЮЧЕНА автоконфигурация VectorStore (не пытается создать EXTENSION)</b></li>
 *   <li>Быстрое выполнение (секунды)</li>
 * </ul>
 *
 * <h2>Ключевые настройки</h2>
 * <ul>
 *   <li>{@code spring.ai.ollama.enabled=false} - отключает Ollama</li>
 *   <li>{@code spring.ai.vectorstore.enabled=false} - отключает VectorStore</li>
 *   <li>{@code spring.autoconfigure.exclude=...PgVectorStoreAutoConfiguration} - исключает автоконфигурацию</li>
 *   <li>H2 in-memory для тестовой базы данных</li>
 *   <li>Все бины заменены на моки через {@code @MockBean}</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 4.2
 * @since 1.0
 */
@Slf4j
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = Application.class,
        properties = {
                // Отключаем AI компоненты для модульных тестов
                "spring.ai.ollama.enabled=false",
                "spring.ai.vectorstore.enabled=false",
                // Отключаем Flyway и SQL инициализацию
                "spring.flyway.enabled=false",
                "spring.sql.init.enabled=false",
                // JPA настройки
                "spring.jpa.hibernate.ddl-auto=create-drop",
                // ⭐ КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: исключаем автоконфигурацию VectorStore
                "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration"
        }
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        // H2 in-memory для модульных тестов
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
public abstract class BaseTest {

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

    @MockBean
    protected EmbeddingModel embeddingModel;

    @Autowired(required = false)
    protected DataSource dataSource;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUpBase() {
        log.info("🚀 Running test: {}", getTestName());
        log.debug("📋 Class: {}", getClass().getName());
        log.debug("📊 Profiles: {}", String.join(", ", environment.getActiveProfiles()));
        assertAllBeansLoaded();
    }

    // ============================================================
    // ПРОВЕРКИ
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

        log.info("✅ All mocks created");
    }

    protected void assertApplicationContextLoaded() {
        assertThat(application)
                .as("Application bean should be loaded")
                .isNotNull();

        assertThat(applicationContext)
                .as("ApplicationContext should be loaded")
                .isNotNull();

        log.info("✅ Context loaded, beans: {}", applicationContext.getBeanDefinitionCount());
    }

    protected void assertDataSourceAvailable() throws SQLException {
        if (dataSource == null) {
            log.warn("⚠️ DataSource not available (optional bean)");
            return;
        }

        try (var connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5))
                    .as("Connection should be valid")
                    .isTrue();

            var metaData = connection.getMetaData();
            log.debug("   📍 URL: {}", metaData.getURL());
            log.debug("   🗄️  Product: {}", metaData.getDatabaseProductName());
            log.debug("   📦 Version: {}", metaData.getDatabaseProductVersion());
            log.info("✅ Database connected");
        }
    }

    protected void assertAllBeansLoaded() {
        assertApplicationContextLoaded();
        assertMocksCreated();
        log.info("✅ All beans loaded");
    }

    // ============================================================
    // УТИЛИТЫ
    // ============================================================

    protected String getTestName() {
        return getClass().getSimpleName();
    }

    protected void logTestStart(String message) {
        log.info("🚀 [{}] {}", getTestName(), message);
    }

    protected void logTestSuccess(String message) {
        log.info("✅ [{}] {}", getTestName(), message);
    }

    protected void logTestWarning(String message) {
        log.warn("⚠️ [{}] {}", getTestName(), message);
    }

    protected void logTestError(String message, Throwable throwable) {
        log.error("❌ [{}] {}", getTestName(), message, throwable);
    }
}