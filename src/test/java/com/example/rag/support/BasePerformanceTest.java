package com.example.rag.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
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
 * операций загрузки и обработки документов. Использует реальные сервисы
 * для измерения времени выполнения операций с документами.</p>
 *
 * <h2>Основные возможности</h2>
 * <ul>
 *   <li>Загрузка Spring контекста с профилем {@code test}</li>
 *   <li>Реальные сервисы {@link DocumentIngestionService} и {@link DocumentRepository}</li>
 *   <li>Моки для всех внешних зависимостей (Ollama, VectorStore)</li>
 *   <li>Автоматический откат транзакций после каждого теста</li>
 *   <li>Тегирование тестов как {@code @Tag("performance")}</li>
 *   <li>Утилиты для измерения времени выполнения</li>
 * </ul>
 *
 * <h2>Иерархия наследования</h2>
 * <pre>
 * BasePerformanceTest
 *     ↑
 *     └── DocumentIngestionServicePerformanceTest
 * </pre>
 *
 * <h2>Аннотации</h2>
 * <ul>
 *   <li>{@code @SpringBootTest} - загружает полный контекст приложения</li>
 *   <li>{@code @ActiveProfiles("test")} - использует тестовый профиль</li>
 *   <li>{@code @Tag("performance")} - маркирует тесты производительности</li>
 *   <li>{@code @TestInstance(Lifecycle.PER_CLASS)} - один экземпляр на класс</li>
 *   <li>{@code @Transactional} - автоматический откат после каждого теста</li>
 * </ul>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * @Slf4j
 * @Tag("performance")
 * class DocumentIngestionServicePerformanceTest extends BasePerformanceTest {
 *
 *     private static final int LARGE_FILE_SIZE = 5 * 1024 * 1024; // 5MB
 *
 *     @Test
 *     @DisplayName("Измерение времени загрузки большого документа")
 *     void testIngestionPerformance() throws Exception {
 *         // Подготовка
 *         var file = createLargeFile("large-document.txt", LARGE_FILE_SIZE);
 *
 *         // Измерение времени выполнения
 *         measureTime("Загрузка большого документа", () -> {
 *             ingestionService.ingestDocument(file, "performance-test");
 *         });
 *
 *         // Проверка результатов
 *         var documents = documentRepository.findAll();
 *         assertThat(documents)
 *                 .as("Документ должен быть сохранен")
 *                 .isNotEmpty();
 *
 *         log.info("✅ Performance test passed");
 *     }
 *
 *     @Test
 *     @DisplayName("Измерение времени загрузки нескольких документов")
 *     void testMultipleDocumentsPerformance() throws Exception {
 *         var files = createMultipleFiles(10);
 *
 *         measureTime("Загрузка 10 документов", () -> {
 *             for (var file : files) {
 *                 ingestionService.ingestDocument(file, "batch-test");
 *             }
 *         });
 *
 *         assertThat(documentRepository.count()).isEqualTo(10);
 *         log.info("✅ Batch performance test passed");
 *     }
 *
 *     private MockMultipartFile createLargeFile(String name, int size) {
 *         var content = "a".repeat(size);
 *         return new MockMultipartFile(
 *             "file",
 *             name,
 *             MediaType.TEXT_PLAIN_VALUE,
 *             content.getBytes()
 *         );
 *     }
 *
 *     private List<MockMultipartFile> createMultipleFiles(int count) {
 *         return IntStream.range(0, count)
 *             .mapToObj(i -> new MockMultipartFile(
 *                 "file",
 *                 "doc-" + i + ".txt",
 *                 MediaType.TEXT_PLAIN_VALUE,
 *                 ("Content " + i).getBytes()
 *             ))
 *             .collect(Collectors.toList());
 *     }
 * }
 * }</pre>
 *
 * <h2>Запуск тестов производительности</h2>
 * <pre>{@code
 * // Запустить все performance тесты
 * mvn test -Dgroups=performance
 *
 * // Запустить конкретный тест
 * mvn test -Dtest=DocumentIngestionServicePerformanceTest
 *
 * // Запустить с профилем
 * mvn test -Dspring.profiles.active=test -Dgroups=performance
 * }</pre>
 *
 * <h2>Важные замечания</h2>
 * <ul>
 *   <li>Тесты производительности могут выполняться долго</li>
 *   <li>Рекомендуется запускать отдельно от обычных тестов</li>
 *   <li>Используется реальная база данных, а не H2</li>
 *   <li>Транзакции автоматически откатываются для изоляции</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 6.0
 * @see DocumentIngestionService
 * @see DocumentRepository
 * @see TestUtils
 * @since 1.0
 */
@Slf4j
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Tag("performance")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public abstract class BasePerformanceTest {

    // ============================================================
    // РЕАЛЬНЫЕ КОМПОНЕНТЫ
    // ============================================================

    /**
     * Сервис для загрузки и обработки документов.
     * <p>
     * Используется реальный бин, а не мок, для измерения реальной
     * производительности операций с документами.
     */
    @Autowired
    protected DocumentIngestionService ingestionService;

    /**
     * Репозиторий для работы с документами в БД.
     * <p>
     * Используется для проверки результатов и очистки данных.
     */
    @Autowired
    protected DocumentRepository documentRepository;

    // ============================================================
    // МОКИ ДЛЯ ВНЕШНИХ ЗАВИСИМОСТЕЙ
    // ============================================================

    /**
     * Мок для {@link RagService}.
     * <p>
     * Заменяет реальный сервис RAG для изоляции тестов производительности.
     */
    @MockBean
    protected RagService ragService;

    /**
     * Мок для {@link OllamaApi}.
     * <p>
     * Заменяет реальный API клиент Ollama для избежания сетевых вызовов.
     */
    @MockBean
    protected OllamaApi ollamaApi;

    /**
     * Мок для {@link OllamaChatModel}.
     * <p>
     * Заменяет реальную модель чата для ускорения тестов.
     */
    @MockBean
    protected OllamaChatModel ollamaChatModel;

    /**
     * Мок для {@link VectorStore}.
     * <p>
     * Заменяет реальное векторное хранилище для изоляции тестов.
     */
    @MockBean
    protected VectorStore vectorStore;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    /**
     * Инициализация перед каждым тестом.
     * <p>
     * Логирует информацию о тесте и проверяет создание моков.
     */
    @BeforeEach
    void setUpBase() {
        log.info("⚡ Performance test: {}", getTestName());
        assertMocksCreated();
        log.info("📊 Document count before test: {}", documentRepository.count());
    }

    // ============================================================
    // ПРОВЕРКИ
    // ============================================================

    /**
     * Проверяет, что все моки созданы корректно.
     * <p>
     * Проверяет:
     * <ul>
     *   <li>{@link RagService}</li>
     *   <li>{@link OllamaApi}</li>
     *   <li>{@link OllamaChatModel}</li>
     *   <li>{@link VectorStore}</li>
     * </ul>
     *
     * @throws AssertionError если любой из моков равен {@code null}
     */
    protected void assertMocksCreated() {
        assertThat(ragService)
                .as("RagService mock should be created")
                .isNotNull();

        assertThat(ollamaApi)
                .as("OllamaApi mock should be created")
                .isNotNull();

        assertThat(ollamaChatModel)
                .as("OllamaChatModel mock should be created")
                .isNotNull();

        assertThat(vectorStore)
                .as("VectorStore mock should be created")
                .isNotNull();

        log.info("✅ All mocks created successfully");
    }

    // ============================================================
    // УТИЛИТЫ
    // ============================================================

    /**
     * Возвращает имя текущего тестового класса.
     *
     * @return имя класса теста
     */
    protected String getTestName() {
        return getClass().getSimpleName();
    }

    /**
     * Логирует начало теста.
     *
     * @param message сообщение для логирования
     */
    protected void logTestStart(String message) {
        log.info("⚡ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует успешное завершение теста.
     *
     * @param message сообщение для логирования
     */
    protected void logTestSuccess(String message) {
        log.info("✅ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует предупреждение в тесте.
     *
     * @param message сообщение для логирования
     */
    protected void logTestWarning(String message) {
        log.warn("⚠️ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует ошибку в тесте.
     *
     * @param message   сообщение для логирования
     * @param throwable исключение для логирования
     */
    protected void logTestError(String message, Throwable throwable) {
        log.error("❌ [{}] {}", getTestName(), message, throwable);
    }

    /**
     * Измеряет время выполнения операции и логирует результат.
     * <p>
     * Использует {@link TestUtils#measureExecutionTime(String, Runnable)}.
     *
     * @param operation название операции
     * @param runnable  код для выполнения
     */
    protected void measureTime(String operation, Runnable runnable) {
        TestUtils.measureExecutionTime(operation, runnable);
    }

    /**
     * Измеряет время выполнения операции с возвратом результата.
     *
     * @param operation название операции
     * @param supplier  код для выполнения
     * @param <T>       тип результата
     * @return результат выполнения
     */
    protected <T> T measureTime(String operation, java.util.function.Supplier<T> supplier) {
        return TestUtils.measureExecutionTime(operation, supplier);
    }

    /**
     * Очищает репозиторий документов.
     * <p>
     * Полезно для изоляции тестов друг от друга.
     */
    protected void clearDocuments() {
        var count = documentRepository.count();
        documentRepository.deleteAll();
        log.debug("🧹 Cleared {} documents", count);
    }

    /**
     * Проверяет, что количество документов соответствует ожидаемому.
     *
     * @param expected ожидаемое количество
     */
    protected void assertDocumentCount(long expected) {
        var actual = documentRepository.count();
        assertThat(actual)
                .as("Document count should be %d", expected)
                .isEqualTo(expected);
        log.debug("📊 Document count: {}", actual);
    }
}