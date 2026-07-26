package com.example.rag.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.example.rag.Application;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.service.DocumentIngestionService;
import com.example.rag.service.RagService;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Базовый класс для тестов производительности.
 *
 * <h2>Назначение</h2>
 * <p>Предоставляет инфраструктуру для тестирования производительности
 * операций загрузки и обработки документов.</p>
 *
 * <h2>Основные возможности</h2>
 * <ul>
 *   <li>Загрузка Spring контекста с профилем {@code test}</li>
 *   <li>Реальные сервисы {@link DocumentIngestionService} и {@link DocumentRepository}</li>
 *   <li>Моки для всех внешних зависимостей</li>
 *   <li>Тегирование тестов как {@code @Tag("performance")}</li>
 * </ul>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * @Tag("performance")
 * @SpringBootTest
 * @ActiveProfiles("test")
 * @Slf4j
 * class DocumentIngestionServicePerformanceTest extends BasePerformanceTest {
 *
 *     @Test
 *     void testIngestionPerformance() throws Exception {
 *         MockMultipartFile file = createLargeFile();
 *         measureTime("Ingestion", () -> ingestionService.ingestDocument(file, "test"));
 *         assertThat(documentRepository.findAll()).isNotEmpty();
 *         log.info("✅ Performance test passed");
 *     }
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 5.0
 * @since 1.0
 */
@Slf4j
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Tag("performance")
public abstract class BasePerformanceTest {

    // ============================================================
    // РЕАЛЬНЫЕ КОМПОНЕНТЫ
    // ============================================================

    @Autowired
    protected DocumentIngestionService ingestionService;

    @Autowired
    protected DocumentRepository documentRepository;

    // ============================================================
    // МОКИ
    // ============================================================

    @MockBean
    protected RagService ragService;

    @MockBean
    protected OllamaApi ollamaApi;

    @MockBean
    protected OllamaChatModel ollamaChatModel;

    @MockBean
    protected VectorStore vectorStore;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUpBase() {
        log.info("⚡ Performance test: {}", getTestName());
        assertMocksCreated();
    }

    // ============================================================
    // ПРОВЕРКИ
    // ============================================================

    protected void assertMocksCreated() {
        assertThat(ragService).isNotNull();
        assertThat(ollamaApi).isNotNull();
        assertThat(ollamaChatModel).isNotNull();
        assertThat(vectorStore).isNotNull();
        log.info("✅ All mocks created");
    }

    // ============================================================
    // УТИЛИТЫ
    // ============================================================

    protected String getTestName() {
        return getClass().getSimpleName();
    }

    protected void logTestStart(String message) {
        log.info("⚡ [{}] {}", getTestName(), message);
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

    protected void measureTime(String operation, Runnable runnable) {
        TestUtils.measureExecutionTime(operation, runnable);
    }
}