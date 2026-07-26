package com.example.rag.performance;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;
import com.example.rag.service.DocumentIngestionService;
import com.example.rag.support.BasePerformanceTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты производительности для сервиса загрузки документов.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет производительность {@link DocumentIngestionService} при различных
 * сценариях загрузки документов.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Загрузка больших документов (до 12KB)</li>
 *   <li>Загрузка документов разных размеров (параметризованный тест)</li>
 *   <li>Конкурентная загрузка множества документов</li>
 * </ul>
 *
 * <h2>Пороговые значения</h2>
 * <ul>
 *   <li>Загрузка одного документа: &lt; 60 секунд</li>
 *   <li>Конкурентная загрузка 10 документов: &lt; 120 секунд</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see DocumentIngestionService
 * @see BasePerformanceTest
 * @since 1.0
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Tag("performance")
@Epic("Тесты производительности")
@Feature("Загрузка документов")
class DocumentIngestionServicePerformanceTest extends BasePerformanceTest {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final long MAX_SINGLE_INGESTION_MS = 60_000;
    private static final long MAX_CONCURRENT_INGESTION_MS = 120_000;
    private static final int CONCURRENT_FILE_COUNT = 10;
    private static final int CONCURRENT_THREAD_COUNT = 5;
    private static final int LARGE_DOCUMENT_SIZE = 1000;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private DocumentIngestionService ingestionService;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        log.info("🧹 Repository cleared");
    }

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка производительности загрузки больших документов")
    @Story("Производительность")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("PERF-001")
    void testIngestionPerformance() {
        logTestStart("Large document ingestion");

        String largeContent = "Spring AI ".repeat(LARGE_DOCUMENT_SIZE);
        MockMultipartFile file = createMultipartFile("performance-support.txt", largeContent);

        long timeMs = measureIngestionTime(file);

        log.info("📄 Size: {} chars", largeContent.length());
        log.info("⏱️ Time: {} ms", timeMs);

        assertThat(timeMs)
                .as("Ingestion time should be less than {} seconds", MAX_SINGLE_INGESTION_MS / 1000)
                .isLessThan(MAX_SINGLE_INGESTION_MS);

        assertThat(documentRepository.findAll())
                .as("Document should be saved")
                .isNotEmpty();

        logTestSuccess("Large document ingestion completed");
        log.info("   Time: {} ms", timeMs);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000, 5000})
    @Description("Проверка производительности с разными размерами документов")
    @Story("Производительность")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("PERF-002")
    void testIngestionPerformanceWithDifferentSizes(int repeatCount) {
        logTestStart("Document size: " + repeatCount + " repetitions");

        String content = "Spring AI ".repeat(repeatCount);
        MockMultipartFile file = createMultipartFile("performance-" + repeatCount + ".txt", content);

        long timeMs = measureIngestionTime(file);

        log.info("📄 Size {}: {} ms", repeatCount, timeMs);

        assertThat(timeMs)
                .as("Ingestion time for size %d should be less than {} seconds",
                        repeatCount, MAX_SINGLE_INGESTION_MS / 1000)
                .isLessThan(MAX_SINGLE_INGESTION_MS);

        logTestSuccess("Document size " + repeatCount + " completed");
        log.info("   Time: {} ms", timeMs);
    }

    @Test
    @Description("Проверка конкурентной загрузки документов")
    @Story("Производительность")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("PERF-003")
    void testConcurrentIngestion() {
        logTestStart("Concurrent document ingestion");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREAD_COUNT);

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            String content = "Concurrent support document.";

            StopWatch stopWatch = new StopWatch("Concurrent ingestion");
            stopWatch.start("Loading " + CONCURRENT_FILE_COUNT + " files concurrently");

            for (int i = 0; i < CONCURRENT_FILE_COUNT; i++) {
                final int index = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        String fileName = "concurrent-" + index + ".txt";
                        String fileContent = content + " " + index;
                        MockMultipartFile file = createMultipartFile(fileName, fileContent);
                        ingestionService.ingestDocument(file, "concurrent-support");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            stopWatch.stop();

            long timeMs = stopWatch.getLastTaskTimeMillis();

            log.info("📄 Loaded {} files concurrently", CONCURRENT_FILE_COUNT);
            log.info("⏱️ Time: {} ms", timeMs);

            assertThat(timeMs)
                    .as("Concurrent ingestion should complete within {} seconds",
                            MAX_CONCURRENT_INGESTION_MS / 1000)
                    .isLessThan(MAX_CONCURRENT_INGESTION_MS);

            assertThat(documentRepository.findAll())
                    .as("All {} documents should be saved", CONCURRENT_FILE_COUNT)
                    .hasSize(CONCURRENT_FILE_COUNT);

            logTestSuccess("Concurrent ingestion completed");
            log.info("   Time: {} ms", timeMs);

        } finally {
            shutdownExecutor(executor);
        }
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Создает MockMultipartFile для тестирования.
     *
     * @param fileName имя файла
     * @param content  содержимое файла
     * @return MockMultipartFile
     */
    private MockMultipartFile createMultipartFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Измеряет время загрузки документа.
     *
     * @param file документ для загрузки
     * @return время выполнения в миллисекундах
     */
    private long measureIngestionTime(MockMultipartFile file) {
        StopWatch stopWatch = new StopWatch("Document ingestion");
        stopWatch.start("Ingestion");
        ingestionService.ingestDocument(file, "performance-support");
        stopWatch.stop();
        return stopWatch.getLastTaskTimeMillis();
    }

    /**
     * Безопасно завершает работу ExecutorService.
     *
     * @param executor ExecutorService для завершения
     */
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}