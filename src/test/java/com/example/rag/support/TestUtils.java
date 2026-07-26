package com.example.rag.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Утилитный класс для тестов с общими вспомогательными методами.
 *
 * <h2>Основные возможности</h2>
 * <ul>
 *   <li>Измерение времени выполнения операций</li>
 *   <li>Проверка свойств окружения</li>
 *   <li>Безопасное получение значений свойств</li>
 *   <li>Параллельное выполнение задач с виртуальными потоками (Java 21+)</li>
 *   <li>Безопасная задержка для тестов</li>
 *   <li>Повторение операций с измерением времени</li>
 * </ul>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * // Синхронное измерение
 * TestUtils.measureExecutionTime("DB query", () -> {
 *     repository.findAll();
 * });
 *
 * // Асинхронное измерение
 * TestUtils.measureExecutionTimeAsync("Async task", () -> {
 *     service.process();
 * });
 *
 * // Повторение операции
 * TestUtils.repeat(5, "API call", () -> {
 *     client.call();
 * });
 *
 * // Параллельное выполнение
 * TestUtils.runParallel(List.of(
 *     () -> service1.process(),
 *     () -> service2.process()
 * ));
 * }</pre>
 *
 * @author RAG Application Team
 * @version 6.0
 * @since 1.0
 */
@Slf4j
public final class TestUtils {

    /**
     * Исполнитель с виртуальными потоками (Java 21+).
     * <p>
     * Виртуальные потоки легковесны и позволяют создавать тысячи потоков
     * без значительных накладных расходов.
     */
    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Приватный конструктор для утилитного класса.
     */
    private TestUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    // ============================================================
    // ИЗМЕРЕНИЕ ВРЕМЕНИ
    // ============================================================

    /**
     * Измеряет время выполнения синхронной операции.
     *
     * @param name     название операции
     * @param runnable код для выполнения
     */
    public static void measureExecutionTime(String name, Runnable runnable) {
        var start = System.nanoTime();
        try {
            runnable.run();
        } finally {
            var duration = System.nanoTime() - start;
            var milliseconds = TimeUnit.NANOSECONDS.toMillis(duration);
            log.info("⏱️ Operation '{}' completed in {} ms", name, milliseconds);
        }
    }

    /**
     * Измеряет время выполнения синхронной операции с возвратом результата.
     *
     * @param name     название операции
     * @param supplier код для выполнения
     * @param <T>      тип результата
     * @return результат выполнения
     */
    public static <T> T measureExecutionTime(String name, Supplier<T> supplier) {
        var start = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            var duration = System.nanoTime() - start;
            var milliseconds = TimeUnit.NANOSECONDS.toMillis(duration);
            log.info("⏱️ Operation '{}' completed in {} ms", name, milliseconds);
        }
    }

    /**
     * Измеряет время выполнения асинхронной операции.
     *
     * @param name     название операции
     * @param runnable код для выполнения
     * @return CompletableFuture
     */
    public static CompletableFuture<Void> measureExecutionTimeAsync(String name, Runnable runnable) {
        return CompletableFuture.runAsync(
                () -> measureExecutionTime(name, runnable),
                VIRTUAL_THREAD_EXECUTOR
        );
    }

    // ============================================================
    // ПОВТОРЕНИЕ ОПЕРАЦИЙ
    // ============================================================

    /**
     * Повторяет операцию несколько раз с измерением времени.
     *
     * @param times    количество повторений
     * @param name     название операции
     * @param runnable код для выполнения
     */
    public static void repeat(int times, String name, Runnable runnable) {
        for (var i = 0; i < times; i++) {
            var iteration = i + 1;
            measureExecutionTime(name + " #" + iteration, runnable);
        }
    }

    /**
     * Повторяет операцию несколько раз асинхронно.
     *
     * @param times    количество повторений
     * @param name     название операции
     * @param runnable код для выполнения
     * @return CompletableFuture
     */
    public static CompletableFuture<Void> repeatAsync(int times, String name, Runnable runnable) {
        return CompletableFuture.runAsync(
                () -> repeat(times, name, runnable),
                VIRTUAL_THREAD_EXECUTOR
        );
    }

    // ============================================================
    // РАБОТА СО СВОЙСТВАМИ
    // ============================================================

    /**
     * Проверяет, установлено ли свойство.
     *
     * @param propertyName имя свойства
     * @return true если свойство установлено и не пустое
     */
    public static boolean isPropertySet(String propertyName) {
        var value = System.getProperty(propertyName);
        return value != null && !value.isEmpty();
    }

    /**
     * Проверяет, установлено ли свойство асинхронно.
     *
     * @param propertyName имя свойства
     * @return CompletableFuture с результатом
     */
    public static CompletableFuture<Boolean> isPropertySetAsync(String propertyName) {
        return CompletableFuture.supplyAsync(
                () -> isPropertySet(propertyName),
                VIRTUAL_THREAD_EXECUTOR
        );
    }

    /**
     * Получает значение свойства или возвращает значение по умолчанию.
     *
     * @param propertyName имя свойства
     * @param defaultValue значение по умолчанию
     * @return значение свойства или значение по умолчанию
     */
    public static String getPropertyOrDefault(String propertyName, String defaultValue) {
        return System.getProperty(propertyName, defaultValue);
    }

    /**
     * Получает значение свойства асинхронно.
     *
     * @param propertyName имя свойства
     * @param defaultValue значение по умолчанию
     * @return CompletableFuture со значением
     */
    public static CompletableFuture<String> getPropertyOrDefaultAsync(String propertyName, String defaultValue) {
        return CompletableFuture.supplyAsync(
                () -> getPropertyOrDefault(propertyName, defaultValue),
                VIRTUAL_THREAD_EXECUTOR
        );
    }

    // ============================================================
    // CI ОКРУЖЕНИЕ
    // ============================================================

    /**
     * Проверяет, выполняется ли код в CI окружении.
     *
     * @return true если в CI окружении
     */
    public static boolean isCiEnvironment() {
        return Boolean.parseBoolean(System.getenv("CI")) ||
               Boolean.parseBoolean(System.getProperty("ci"));
    }

    /**
     * Проверяет, выполняется ли код в CI окружении асинхронно.
     *
     * @return CompletableFuture с результатом
     */
    public static CompletableFuture<Boolean> isCiEnvironmentAsync() {
        return CompletableFuture.supplyAsync(
                TestUtils::isCiEnvironment,
                VIRTUAL_THREAD_EXECUTOR
        );
    }

    // ============================================================
    // БЕЗОПАСНАЯ ЗАДЕРЖКА
    // ============================================================

    /**
     * Безопасная задержка с обработкой InterruptedException.
     *
     * @param milliseconds время задержки в миллисекундах
     */
    public static void sleepQuietly(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted", e);
        }
    }

    /**
     * Безопасная асинхронная задержка.
     *
     * @param milliseconds время задержки в миллисекундах
     * @return CompletableFuture
     */
    public static CompletableFuture<Void> sleepQuietlyAsync(long milliseconds) {
        return CompletableFuture.runAsync(
                () -> sleepQuietly(milliseconds),
                VIRTUAL_THREAD_EXECUTOR
        );
    }

    // ============================================================
    // ПАРАЛЛЕЛЬНОЕ ВЫПОЛНЕНИЕ
    // ============================================================

    /**
     * Выполняет задачи параллельно с виртуальными потоками.
     *
     * @param tasks список задач
     * @return CompletableFuture
     */
    public static CompletableFuture<Void> runParallel(Iterable<Runnable> tasks) {
        var futures = new ArrayList<CompletableFuture<Void>>();
        for (Runnable task : tasks) {
            futures.add(CompletableFuture.runAsync(task, VIRTUAL_THREAD_EXECUTOR));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Выполняет задачи параллельно и возвращает результаты.
     * <p>
     * Использует виртуальные потоки (Java 21+) для эффективного параллельного выполнения.
     *
     * @param suppliers список поставщиков результатов
     * @param <T>       тип результата
     * @return CompletableFuture со списком результатов
     * @throws RuntimeException если любая из задач завершится с ошибкой
     */
    @SafeVarargs
    public static <T> CompletableFuture<List<T>> runParallelWithResults(
            java.util.concurrent.Callable<T>... suppliers
    ) {
        var futures = new ArrayList<CompletableFuture<T>>(suppliers.length);

        for (var i = 0; i < suppliers.length; i++) {
            final var index = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return suppliers[index].call();
                } catch (Exception e) {
                    log.error("Task #{} failed: {}", index, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }, VIRTUAL_THREAD_EXECUTOR));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    var results = new ArrayList<T>(futures.size());
                    for (var future : futures) {
                        results.add(future.join());
                    }
                    log.debug("✅ All {} parallel tasks completed successfully", results.size());
                    return results;
                });
    }

    // ============================================================
    // ЗАВЕРШЕНИЕ РАБОТЫ
    // ============================================================

    /**
     * Завершает работу исполнителя виртуальных потоков.
     * <p>
     * Должен вызываться в конце тестов для корректного завершения.
     */
    public static void shutdown() {
        VIRTUAL_THREAD_EXECUTOR.shutdown();
        try {
            if (!VIRTUAL_THREAD_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                VIRTUAL_THREAD_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            VIRTUAL_THREAD_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.debug("✅ Virtual thread executor shut down");
    }
}