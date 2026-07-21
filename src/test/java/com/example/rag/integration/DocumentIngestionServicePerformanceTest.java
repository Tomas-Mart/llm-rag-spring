package com.example.rag.integration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест производительности для проверки загрузки документов.
 *
 * <p>Данный класс проверяет производительность сервиса {@link DocumentIngestionService}
 * при различных сценариях загрузки документов. Тесты измеряют время выполнения
 * операций индексации документов в различных условиях.</p>
 *
 * <p>Тестируемые сценарии:
 * <ul>
 *   <li>Загрузка больших документов (до 12KB)</li>
 *   <li>Загрузка документов разных размеров (параметризованный тест)</li>
 *   <li>Конкурентная загрузка множества документов</li>
 * </ul>
 * </p>
 *
 * <p>Все тесты используют моки для внешних зависимостей, что обеспечивает:
 * <ul>
 *   <li>Изоляцию тестов от реальной базы данных</li>
 *   <li>Стабильность и воспроизводимость результатов</li>
 *   <li>Быстрое выполнение тестов</li>
 * </ul>
 * </p>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see BasePerformanceTest
 * @see DocumentIngestionService
 * @since 1.0
 */
@Tag("performance")
@Epic("Тесты производительности")
@Feature("Загрузка документов")
class DocumentIngestionServicePerformanceTest extends BasePerformanceTest {

    /**
     * Сервис для загрузки и индексации документов.
     * Автоматически внедряется Spring.
     */
    @Autowired
    private DocumentIngestionService ingestionService;

    /**
     * Проверяет производительность загрузки больших документов.
     *
     * <p>Тест создает документ размером около 12KB (1000 повторений строки "Spring AI ")
     * и измеряет время его загрузки. Тест считается успешным, если время загрузки
     * не превышает 60 секунд.</p>
     *
     * <p>Метрики:
     * <ul>
     *   <li>Время загрузки в миллисекундах</li>
     *   <li>Размер документа в символах</li>
     * </ul>
     * </p>
     *
     * @throws Exception если возникает ошибка при создании файла или загрузке документа
     */
    @Test
    @Description("Проверка производительности загрузки больших документов")
    @Story("Производительность")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("PERF-001")
    void testIngestionPerformance() throws Exception {
        // Проверяем, что все моки созданы корректно
        assertMocksCreated();

        // Создаем большой документ (~12KB)
        // 1000 повторений строки "Spring AI" дают примерно 12KB текста
        String largeContent = "Spring AI ".repeat(1000);

        // Создаем MultipartFile из строкового содержимого
        MockMultipartFile file = new MockMultipartFile(
                "file",                                          // Имя поля формы
                "performance-support.txt",                       // Имя файла
                "text/plain",                                   // MIME тип
                largeContent.getBytes(StandardCharsets.UTF_8)   // Содержимое файла
        );

        // Создаем секундомер для измерения времени выполнения
        StopWatch stopWatch = new StopWatch("Document ingestion");
        stopWatch.start("Single document ingestion");

        // Выполняем загрузку документа
        ingestionService.ingestDocument(file, "performance-support");

        // Останавливаем секундомер
        stopWatch.stop();

        // Получаем время выполнения в миллисекундах
        long timeMs = stopWatch.getLastTaskTimeMillis();

        // Логируем результаты измерения
        logger.info("⏱️ {}: {} ms", stopWatch.getLastTaskName(), timeMs);
        logger.info("📄 Document size: {} characters", largeContent.length());

        // Проверяем, что время загрузки не превышает 60 секунд
        assertThat(timeMs)
                .as("Ingestion time should be less than 60 seconds")
                .isLessThan(60000);
    }

    /**
     * Проверяет производительность загрузки документов разных размеров.
     *
     * <p>Параметризованный тест, который выполняется с разными размерами документов:
     * <ul>
     *   <li>100 повторений (~1.2KB)</li>
     *   <li>500 повторений (~6KB)</li>
     *   <li>1000 повторений (~12KB)</li>
     *   <li>5000 повторений (~60KB)</li>
     * </ul>
     * </p>
     *
     * <p>Это позволяет оценить, как размер документа влияет на время загрузки.
     * Тест считается успешным, если время загрузки для любого размера
     * не превышает 60 секунд.</p>
     *
     * @param repeatCount количество повторений строки "Spring AI "
     * @throws Exception если возникает ошибка при создании файла или загрузке документа
     */
    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000, 5000})
    @Description("Проверка производительности с разными размерами документов")
    @Story("Производительность")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("PERF-002")
    void testIngestionPerformanceWithDifferentSizes(int repeatCount) throws Exception {
        // Проверяем, что все моки созданы корректно
        assertMocksCreated();

        // Создаем документ с заданным количеством повторений
        String content = "Spring AI ".repeat(repeatCount);

        // Создаем MultipartFile с уникальным именем для каждого размера
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "performance-" + repeatCount + ".txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        // Создаем секундомер для измерения времени выполнения
        StopWatch stopWatch = new StopWatch("Document ingestion");
        stopWatch.start("Size: " + repeatCount);

        // Выполняем загрузку документа
        ingestionService.ingestDocument(file, "performance-support");

        // Останавливаем секундомер
        stopWatch.stop();

        // Получаем время выполнения в миллисекундах
        long timeMs = stopWatch.getLastTaskTimeMillis();

        // Логируем результаты измерения
        logger.info("⏱️ Size {}: {} ms", repeatCount, timeMs);

        // Проверяем, что время загрузки не превышает 60 секунд
        assertThat(timeMs)
                .as("Ingestion time for size %d should be less than 60 seconds", repeatCount)
                .isLessThan(60000);
    }

    /**
     * Проверяет производительность конкурентной загрузки множества документов.
     *
     * <p>Тест загружает 10 документов одновременно в 5 параллельных потоков.
     * Использует {@link ExecutorService} и {@link CompletableFuture} для
     * асинхронного выполнения.</p>
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Возможность параллельной загрузки документов</li>
     *   <li>Отсутствие ошибок при конкурентном доступе</li>
     *   <li>Время выполнения всех операций (не должно превышать 120 секунд)</li>
     * </ul>
     * </p>
     *
     * <p>Используется пул из 5 потоков для имитации реальной нагрузки
     * и проверки потокобезопасности сервиса.</p>
     *
     * @throws Exception если возникает ошибка при создании файлов или загрузке документов
     */
    @Test
    @Description("Проверка конкурентной загрузки документов")
    @Story("Производительность")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("PERF-003")
    void testConcurrentIngestion() throws Exception {
        // Проверяем, что все моки созданы корректно
        assertMocksCreated();

        // Количество файлов для конкурентной загрузки
        int numberOfFiles = 10;

        // Базовое содержимое для всех файлов
        String content = "Concurrent support document.";

        // Создаем пул потоков для параллельного выполнения
        // Используем 5 потоков для имитации реальной нагрузки
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // Список для хранения CompletableFuture задач
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Создаем секундомер для измерения времени выполнения
        StopWatch stopWatch = new StopWatch("Concurrent ingestion");
        stopWatch.start("Loading " + numberOfFiles + " files concurrently");

        // Запускаем задачи для каждого файла
        for (int i = 0; i < numberOfFiles; i++) {
            final int index = i;

            // Создаем асинхронную задачу
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Создаем файл с уникальным именем и содержимым
                    MockMultipartFile file = new MockMultipartFile(
                            "file",
                            "concurrent-" + index + ".txt",
                            "text/plain",
                            (content + " " + index).getBytes(StandardCharsets.UTF_8)
                    );

                    // Выполняем загрузку документа
                    ingestionService.ingestDocument(file, "concurrent-support");
                } catch (Exception exception) {
                    // В случае ошибки оборачиваем в RuntimeException
                    throw new RuntimeException(exception);
                }
            }, executor);

            // Добавляем задачу в список
            futures.add(future);
        }

        // Ожидаем завершения всех задач
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        // Останавливаем секундомер
        stopWatch.stop();

        // Корректно завершаем работу пула потоков
        executor.shutdown();
        // Ожидаем завершения всех потоков (максимум 5 секунд)
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Логируем результаты измерения
        logger.info("⏱️ {}: {} ms", stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis());
        logger.info("📄 Loaded {} files concurrently", numberOfFiles);

        // Проверяем, что время загрузки всех файлов не превышает 120 секунд
        // Даем больше времени для конкурентной загрузки
        assertThat(stopWatch.getLastTaskTimeMillis())
                .as("Concurrent ingestion should complete within 120 seconds")
                .isLessThan(120000);
    }
}