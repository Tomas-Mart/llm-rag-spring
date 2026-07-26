package com.example.rag.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * </ul>
 *
 * @author RAG Application Team
 * @version 5.0
 * @since 1.0
 */
public final class TestUtils {

    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private TestUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    // ============================================================
    // ИЗМЕРЕНИЕ ВРЕМЕНИ
    // ============================================================

    public static void measureExecutionTime(String operation, Runnable runnable) {
        long startTime = System.nanoTime();
        try {
            runnable.run();
        } finally {
            long duration = System.nanoTime() - startTime;
            long milliseconds = TimeUnit.NANOSECONDS.toMillis(duration);
            logger.info("⏱️ Operation '{}' completed in {} ms", operation, milliseconds);
        }
    }

    public static CompletableFuture<Void> measureExecutionTimeAsync(String operation, Runnable runnable) {
        return CompletableFuture.runAsync(() -> measureExecutionTime(operation, runnable), VIRTUAL_THREAD_EXECUTOR);
    }

    // ============================================================
    // РАБОТА СО СВОЙСТВАМИ
    // ============================================================

    public static boolean isPropertySet(String propertyName) {
        String value = System.getProperty(propertyName);
        return value != null && !value.isEmpty();
    }

    public static CompletableFuture<Boolean> isPropertySetAsync(String propertyName) {
        return CompletableFuture.supplyAsync(() -> isPropertySet(propertyName), VIRTUAL_THREAD_EXECUTOR);
    }

    public static String getPropertyOrDefault(String propertyName, String defaultValue) {
        return System.getProperty(propertyName, defaultValue);
    }

    public static CompletableFuture<String> getPropertyOrDefaultAsync(String propertyName, String defaultValue) {
        return CompletableFuture.supplyAsync(
                () -> getPropertyOrDefault(propertyName, defaultValue),
                VIRTUAL_THREAD_EXECUTOR
        );
    }

    // ============================================================
    // CI ОКРУЖЕНИЕ
    // ============================================================

    public static boolean isCiEnvironment() {
        return Boolean.parseBoolean(System.getenv("CI")) ||
               Boolean.parseBoolean(System.getProperty("ci"));
    }

    public static CompletableFuture<Boolean> isCiEnvironmentAsync() {
        return CompletableFuture.supplyAsync(TestUtils::isCiEnvironment, VIRTUAL_THREAD_EXECUTOR);
    }

    // ============================================================
    // БЕЗОПАСНАЯ ЗАДЕРЖКА
    // ============================================================

    public static void sleepQuietly(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep interrupted", e);
        }
    }

    public static CompletableFuture<Void> sleepQuietlyAsync(long milliseconds) {
        return CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Sleep interrupted", e);
            }
        }, VIRTUAL_THREAD_EXECUTOR);
    }

    // ============================================================
    // ПАРАЛЛЕЛЬНОЕ ВЫПОЛНЕНИЕ
    // ============================================================

    public static CompletableFuture<Void> runParallel(Iterable<Runnable> tasks) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Runnable task : tasks) {
            futures.add(CompletableFuture.runAsync(task, VIRTUAL_THREAD_EXECUTOR));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @SafeVarargs
    public static <T> CompletableFuture<List<T>> runParallelWithResults(java.util.concurrent.Callable<T>... suppliers) {
        @SuppressWarnings("unchecked")
        CompletableFuture<T>[] futures = new CompletableFuture[suppliers.length];

        for (int i = 0; i < suppliers.length; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return suppliers[index].call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, VIRTUAL_THREAD_EXECUTOR);
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    List<T> results = new ArrayList<>();
                    for (CompletableFuture<T> future : futures) {
                        results.add(future.join());
                    }
                    return results;
                });
    }

    // ============================================================
    // ЗАВЕРШЕНИЕ РАБОТЫ
    // ============================================================

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
    }
}