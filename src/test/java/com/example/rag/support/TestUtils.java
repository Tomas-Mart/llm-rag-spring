package com.example.rag.support;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утилитный класс для тестов с общими вспомогательными методами.
 *
 * <p>Предоставляет набор статических методов для упрощения написания тестов:
 * <ul>
 *   <li>Измерение времени выполнения операций</li>
 *   <li>Проверка свойств окружения</li>
 *   <li>Безопасное получение значений свойств</li>
 * </ul>
 *
 * <p>Класс является финальным и не может быть наследован.
 * Конструктор приватный для предотвращения создания экземпляров.
 *
 * <p>Пример использования:
 * <pre>{@code
 * // Измерение времени выполнения
 * TestUtils.measureExecutionTime("Database query", () -> {
 *     // Выполнение операции
 * });
 *
 * // Проверка свойства окружения
 * if (TestUtils.isPropertySet("TEST_ENV")) {
 *     String value = TestUtils.getPropertyOrDefault("TEST_ENV", "default");
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
public final class TestUtils {

    /**
     * Логгер для утилитного класса.
     */
    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Исполнитель для асинхронных операций с виртуальными потоками.
     * Использует виртуальные потоки (Java 21+) для эффективного выполнения большого количества задач.
     */
    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Приватный конструктор для предотвращения создания экземпляров.
     *
     * @throws UnsupportedOperationException всегда, так как класс утилитный
     */
    private TestUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Измеряет время выполнения операции и логирует результат.
     *
     * <p>Метод измеряет время выполнения переданной операции в наносекундах
     * и преобразует результат в миллисекунды для удобочитаемости.
     * Результат логируется с использованием SLF4J.
     *
     * <p>Пример использования:
     * <pre>{@code
     * TestUtils.measureExecutionTime("Database connection", () -> {
     *     // Код для измерения времени выполнения
     *     dataSource.getConnection();
     * });
     * }</pre>
     *
     * @param operation описание операции для логирования
     * @param runnable  выполняемая операция в виде {@link Runnable}
     */
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

    /**
     * Асинхронно измеряет время выполнения операции и логирует результат.
     *
     * <p>Метод выполняет операцию в виртуальном потоке и возвращает {@link CompletableFuture}.
     * Время выполнения измеряется в наносекундах и преобразуется в миллисекунды.
     *
     * <p>Пример использования:
     * <pre>{@code
     * CompletableFuture<Void> future = TestUtils.measureExecutionTimeAsync("Async query", () -> {
     *     // Длительная операция
     * });
     * future.join();
     * }</pre>
     *
     * @param operation описание операции для логирования
     * @param runnable  выполняемая операция в виде {@link Runnable}
     * @return {@link CompletableFuture} для ожидания завершения операции
     */
    public static CompletableFuture<Void> measureExecutionTimeAsync(String operation, Runnable runnable) {
        return CompletableFuture.runAsync(() -> {
            measureExecutionTime(operation, runnable);
        }, VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Проверяет, что свойство окружения установлено.
     *
     * <p>Метод проверяет наличие системного свойства и что его значение
     * не является пустой строкой.
     *
     * <p>Пример использования:
     * <pre>{@code
     * if (TestUtils.isPropertySet("TEST_ENV")) {
     *     // Свойство установлено
     * }
     * }</pre>
     *
     * @param propertyName имя свойства для проверки
     * @return {@code true} если свойство установлено и не пустое,
     * {@code false} в противном случае
     */
    public static boolean isPropertySet(String propertyName) {
        String value = System.getProperty(propertyName);
        return value != null && !value.isEmpty();
    }

    /**
     * Асинхронно проверяет, что свойство окружения установлено.
     *
     * <p>Метод проверяет наличие системного свойства в виртуальном потоке.
     *
     * @param propertyName имя свойства для проверки
     * @return {@link CompletableFuture} с результатом {@code true} если свойство установлено,
     * {@code false} в противном случае
     */
    public static CompletableFuture<Boolean> isPropertySetAsync(String propertyName) {
        return CompletableFuture.supplyAsync(() -> isPropertySet(propertyName), VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Безопасно получает значение свойства или возвращает значение по умолчанию.
     *
     * <p>Метод пытается получить значение системного свойства.
     * Если свойство не найдено, возвращается переданное значение по умолчанию.
     *
     * <p>Пример использования:
     * <pre>{@code
     * String host = TestUtils.getPropertyOrDefault("OLLAMA_HOST", "localhost");
     * String port = TestUtils.getPropertyOrDefault("OLLAMA_PORT", "11434");
     * }</pre>
     *
     * @param propertyName имя свойства для получения
     * @param defaultValue значение по умолчанию, если свойство не найдено
     * @return значение свойства или значение по умолчанию
     */
    public static String getPropertyOrDefault(String propertyName, String defaultValue) {
        return System.getProperty(propertyName, defaultValue);
    }

    /**
     * Асинхронно получает значение свойства или возвращает значение по умолчанию.
     *
     * <p>Метод пытается получить значение системного свойства в виртуальном потоке.
     *
     * @param propertyName имя свойства для получения
     * @param defaultValue значение по умолчанию, если свойство не найдено
     * @return {@link CompletableFuture} со значением свойства или значением по умолчанию
     */
    public static CompletableFuture<String> getPropertyOrDefaultAsync(String propertyName, String defaultValue) {
        return CompletableFuture.supplyAsync(() -> getPropertyOrDefault(propertyName, defaultValue), VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Проверяет, запущен ли тест в CI окружении.
     *
     * <p>Метод проверяет наличие переменных окружения и системных свойств,
     * характерных для CI систем.
     *
     * @return {@code true} если тест запущен в CI окружении,
     * {@code false} в противном случае
     */
    public static boolean isCiEnvironment() {
        return Boolean.parseBoolean(System.getenv("CI")) ||
               Boolean.parseBoolean(System.getProperty("ci"));
    }

    /**
     * Асинхронно проверяет, запущен ли тест в CI окружении.
     *
     * @return {@link CompletableFuture} с результатом {@code true} если тест запущен в CI,
     * {@code false} в противном случае
     */
    public static CompletableFuture<Boolean> isCiEnvironmentAsync() {
        return CompletableFuture.supplyAsync(TestUtils::isCiEnvironment, VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Безопасная задержка для тестов с паузой.
     *
     * <p>Метод выполняет задержку на указанное количество миллисекунд.
     * В случае прерывания потока, восстанавливает статус прерывания
     * и логирует предупреждение.
     *
     * @param milliseconds время задержки в миллисекундах
     */
    public static void sleepQuietly(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep operation was interrupted", interruptedException);
        }
    }

    /**
     * Асинхронная безопасная задержка для тестов с паузой.
     *
     * <p>Метод выполняет задержку на указанное количество миллисекунд в виртуальном потоке.
     * В случае прерывания потока, восстанавливает статус прерывания
     * и логирует предупреждение.
     *
     * @param milliseconds время задержки в миллисекундах
     * @return {@link CompletableFuture} для ожидания завершения задержки
     */
    public static CompletableFuture<Void> sleepQuietlyAsync(long milliseconds) {
        return CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(milliseconds);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                logger.warn("Sleep operation was interrupted", interruptedException);
            }
        }, VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Выполняет несколько операций параллельно с использованием виртуальных потоков.
     *
     * <p>Метод принимает список задач и выполняет их параллельно в виртуальных потоках.
     * Возвращает {@link CompletableFuture}, который завершается после выполнения всех задач.
     *
     * <p>Пример использования:
     * <pre>{@code
     * List<Runnable> tasks = Arrays.asList(
     *     () -> System.out.println("Task 1"),
     *     () -> System.out.println("Task 2"),
     *     () -> System.out.println("Task 3")
     * );
     * TestUtils.runParallel(tasks).join();
     * }</pre>
     *
     * @param tasks список задач для параллельного выполнения
     * @return {@link CompletableFuture} для ожидания завершения всех задач
     */
    public static CompletableFuture<Void> runParallel(Iterable<Runnable> tasks) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[0];

        java.util.stream.StreamSupport.stream(tasks.spliterator(), false)
                .map(task -> CompletableFuture.runAsync(task, VIRTUAL_THREAD_EXECUTOR))
                .reduce(CompletableFuture.allOf(futures), (future1, future2) ->
                        CompletableFuture.allOf(future1, future2));

        return CompletableFuture.allOf(futures);
    }

    /**
     * Выполняет несколько операций параллельно с использованием виртуальных потоков
     * и собирает результаты.
     *
     * <p>Метод принимает список поставщиков и выполняет их параллельно в виртуальных потоках.
     * Возвращает {@link CompletableFuture} со списком результатов.
     *
     * @param suppliers список поставщиков для параллельного выполнения
     * @param <T>       тип результата
     * @return {@link CompletableFuture} со списком результатов
     */
    @SafeVarargs
    public static <T> CompletableFuture<java.util.List<T>> runParallelWithResults(java.util.concurrent.Callable<T>... suppliers) {
        @SuppressWarnings("unchecked")
        CompletableFuture<T>[] futures = new CompletableFuture[suppliers.length];

        for (int i = 0; i < suppliers.length; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return suppliers[index].call();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }, VIRTUAL_THREAD_EXECUTOR);
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    java.util.List<T> results = new java.util.ArrayList<>();
                    for (CompletableFuture<T> future : futures) {
                        results.add(future.join());
                    }
                    return results;
                });
    }

    /**
     * Закрывает исполнитель виртуальных потоков.
     *
     * <p>Должен быть вызван при завершении работы приложения для корректного
     * завершения всех виртуальных потоков.
     */
    public static void shutdown() {
        VIRTUAL_THREAD_EXECUTOR.shutdown();
        try {
            if (!VIRTUAL_THREAD_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                VIRTUAL_THREAD_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            VIRTUAL_THREAD_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}