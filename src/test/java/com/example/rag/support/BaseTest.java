package com.example.rag.support;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
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
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Базовый класс для модульных тестов.
 *
 * <h2>Основные возможности</h2>
 * <ul>
 *   <li>Загрузка Spring контекста с профилем {@code test}</li>
 *   <li>Моки для всех внешних зависимостей</li>
 *   <li>Подключение к существующему контейнеру PostgreSQL</li>
 *   <li>Быстрое выполнение благодаря мокам</li>
 * </ul>
 *
 * <h2>Моки для внешних зависимостей</h2>
 * <ul>
 *   <li>{@link OllamaApi} - клиент для Ollama API</li>
 *   <li>{@link OllamaChatModel} - модель чата</li>
 *   <li>{@link VectorStore} - векторное хранилище</li>
 *   <li>{@link ChatClient} - клиент для работы с чатом</li>
 *   <li>{@link EmbeddingModel} - модель эмбеддингов</li>
 * </ul>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * @SpringBootTest
 * @ActiveProfiles("test")
 * @Slf4j
 * class MyServiceTest extends BaseTest {
 *
 *     @Autowired
 *     private MyService myService;
 *
 *     @Test
 *     void testServiceMethod() {
 *         when(vectorStore.similaritySearch(any())).thenReturn(List.of(...));
 *         String result = myService.process();
 *         assertThat(result).isNotNull();
 *         log.info("✅ Test passed");
 *     }
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 5.0
 * @since 1.0
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
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
        log.debug("🐘 PostgreSQL: {}", environment.getProperty("spring.datasource.url"));
        log.debug("📊 Profiles: {}", String.join(", ", environment.getActiveProfiles()));
        assertAllBeansLoaded();
    }

    // ============================================================
    // ПРОВЕРКИ
    // ============================================================

    protected void assertMocksCreated() {
        assertThat(ollamaApi).isNotNull();
        assertThat(ollamaChatModel).isNotNull();
        assertThat(vectorStore).isNotNull();
        assertThat(chatClient).isNotNull();
        assertThat(embeddingModel).isNotNull();
        log.info("✅ All mocks created");
    }

    protected void assertApplicationContextLoaded() {
        assertThat(application).isNotNull();
        assertThat(applicationContext).isNotNull();
        log.info("✅ Context loaded, beans: {}", applicationContext.getBeanDefinitionCount());
    }

    protected void assertDataSourceAvailable() throws SQLException {
        if (dataSource == null) {
            log.warn("⚠️ DataSource not available");
            return;
        }
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn.isValid(5)).isTrue();
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