package com.example.rag.support;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.example.rag.Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Абстрактный базовый класс для всех тестов приложения.
 * Обеспечивает единую конфигурацию Spring контекста и общие моки для тестирования.
 *
 * <p>Основные возможности:
 * <ul>
 *   <li>Загрузка Spring контекста с тестовым профилем {@code test}</li>
 *   <li>Предоставление общих моков для внешних зависимостей (Ollama, Vector Store)</li>
 *   <li>Содержит общие методы для проверки контекста, моков и подключения к базе данных</li>
 *   <li>Использует SLF4J для структурированного логирования</li>
 *   <li>Автоматическое логирование начала выполнения каждого теста</li>
 * </ul>
 *
 * <p>Пример использования:
 * <pre>{@code
 * @SpringBootTest
 * class MyServiceTest extends BaseTest {
 *
 *     @Autowired
 *     private MyService myService;
 *
 *     @Test
 *     void testServiceMethod() {
 *         // Проверяем загрузку контекста и моков
 *         assertAllBeansLoaded();
 *
 *         // Выполняем тест
 *         String result = myService.process();
 *         assertThat(result).isNotNull();
 *     }
 * }
 * }</pre>
 *
 * <p>Аннотации класса:
 * <ul>
 *   <li>{@code @SpringBootTest} - загружает полный Spring контекст</li>
 *   <li>{@code @ActiveProfiles("test")} - активирует тестовый профиль</li>
 *   <li>{@code @TestPropertySource} - переопределяет свойства для тестов</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see Application
 * @see OllamaApi
 * @see OllamaChatModel
 * @see VectorStore
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.ai.ollama.base-url=http://localhost:11434",
        "spring.ai.ollama.chat.options.model=qwen2.5-coder:7b"
})
public abstract class BaseTest {

    /**
     * Логгер для всех тестовых классов.
     * Используется для единообразного логирования во всех наследниках.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Главный Application bean для проверки загрузки контекста.
     * Автоматически внедряется Spring.
     */
    @Autowired
    protected Application application;

    /**
     * Мок для Ollama API.
     * Используется для изоляции тестов от реального Ollama сервера.
     *
     * <p>Позволяет:
     * <ul>
     *   <li>Тестировать без запущенного Ollama сервера</li>
     *   <li>Контролировать поведение API в тестах</li>
     *   <li>Ускорять выполнение тестов</li>
     * </ul>
     */
    @MockBean
    protected OllamaApi ollamaApi;

    /**
     * Мок для Ollama Chat Model.
     * Используется для изоляции тестов от реальной LLM.
     *
     * <p>Позволяет:
     * <ul>
     *   <li>Тестировать без реальных LLM запросов</li>
     *   <li>Мокать ответы модели</li>
     *   <li>Тестировать обработку ошибок</li>
     * </ul>
     */
    @MockBean
    protected OllamaChatModel ollamaChatModel;

    /**
     * Мок для Vector Store.
     * Используется для изоляции тестов от реальной векторной базы данных.
     *
     * <p>Позволяет:
     * <ul>
     *   <li>Тестировать без реальной векторной базы данных</li>
     *   <li>Контролировать операции с векторами</li>
     *   <li>Ускорять выполнение тестов</li>
     * </ul>
     */
    @MockBean
    protected VectorStore vectorStore;

    /**
     * DataSource для проверки подключения к базе данных.
     * Может быть {@code null} в тестах, где база данных не требуется.
     *
     * <p>Используется для:
     * <ul>
     *   <li>Проверки наличия подключения к базе данных</li>
     *   <li>Выполнения тестовых запросов</li>
     *   <li>Проверки конфигурации базы данных</li>
     * </ul>
     */
    @Autowired(required = false)
    protected DataSource dataSource;

    /**
     * Инициализация перед каждым тестом.
     * Выводит информацию о запуске теста в лог.
     *
     * <p>Логирует:
     * <ul>
     *   <li>Начало выполнения теста с эмодзи 🚀</li>
     *   <li>Имя класса теста для идентификации</li>
     *   <li>Текущее время выполнения</li>
     * </ul>
     */
    @BeforeEach
    void setUpBase() {
        logger.info("🚀 Running test: {} - Starting execution", getTestName());
        logger.debug("📋 Test class: {}", getClass().getName());
        logger.debug("🕐 Current timestamp: {}", System.currentTimeMillis());
    }

    /**
     * Проверяет, что все основные моки созданы и не равны {@code null}.
     *
     * <p>Проверяемые моки:
     * <ul>
     *   <li>{@link #ollamaApi} - мок Ollama API</li>
     *   <li>{@link #ollamaChatModel} - мок Ollama Chat Model</li>
     *   <li>{@link #vectorStore} - мок Vector Store</li>
     * </ul>
     *
     * <p>Должен вызываться в каждом тестовом классе для гарантии,
     * что Spring создал все необходимые моки.
     *
     * <p>Пример использования:
     * <pre>{@code
     * @Test
     * void testSomething() {
     *     assertMocksCreated();
     *     // Дальнейшие проверки
     * }
     * }</pre>
     *
     * @throws AssertionError если любой из моков равен {@code null}
     */
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

        logger.info("All mocks created successfully");
        logger.debug("   - OllamaApi: {}", ollamaApi.getClass().getSimpleName());
        logger.debug("   - OllamaChatModel: {}", ollamaChatModel.getClass().getSimpleName());
        logger.debug("   - VectorStore: {}", vectorStore.getClass().getSimpleName());
    }

    /**
     * Проверяет, что Application контекст загружен.
     *
     * <p>Убеждается, что главный {@link Application} bean был создан
     * и внедрен в тестовый класс.
     *
     * <p>Эта проверка гарантирует:
     * <ul>
     *   <li>Корректная загрузка Spring контекста</li>
     *   <li>Все бины созданы и зарегистрированы</li>
     *   <li>Конфигурация приложения корректна</li>
     * </ul>
     *
     * @throws AssertionError если {@link #application} равен {@code null}
     */
    protected void assertApplicationContextLoaded() {
        assertThat(application)
                .as("Application bean should be loaded")
                .isNotNull();
        logger.info("Application context loaded successfully");
    }

    /**
     * Проверяет, что DataSource доступен и можно установить соединение с базой данных.
     *
     * <p>Выполняет следующие проверки:
     * <ul>
     *   <li>DataSource не равен {@code null}</li>
     *   <li>Успешное установление соединения</li>
     *   <li>Валидность соединения (таймаут 5 секунд)</li>
     *   <li>Получение метаданных базы данных</li>
     * </ul>
     *
     * <p>Если DataSource не доступен (равен {@code null}),
     * метод логирует предупреждение и завершается без ошибки.
     *
     * <p>Пример использования:
     * <pre>{@code
     * @Test
     * void testDatabaseConnection() throws SQLException {
     *     assertDataSourceAvailable();
     *     // Дальнейшие операции с базой данных
     * }
     * }</pre>
     *
     * @throws SQLException   если ошибка подключения к базе данных или проверки соединения
     * @throws AssertionError если соединение невалидно
     */
    protected void assertDataSourceAvailable() throws SQLException {
        if (dataSource == null) {
            logger.warn("DataSource is not available in current context");
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
            logger.info("Database connection established successfully");
            logger.debug("   Database URL: {}", metaData.getURL());
            logger.debug("   Database Product: {}", metaData.getDatabaseProductName());
            logger.debug("   Database Version: {}", metaData.getDatabaseProductVersion());
            logger.debug("   JDBC Driver: {}", metaData.getDriverName());
            logger.debug("   JDBC Version: {}", metaData.getDriverVersion());
        } catch (SQLException sqlException) {
            logger.error("Failed to connect to database", sqlException);
            throw sqlException;
        }
    }

    /**
     * Проверяет, что все обязательные бины загружены.
     * Объединяет проверку контекста и моков в одном методе.
     *
     * <p>Выполняет последовательно:
     * <ol>
     *   <li>{@link #assertApplicationContextLoaded()} - проверка контекста</li>
     *   <li>{@link #assertMocksCreated()} - проверка моков</li>
     * </ol>
     *
     * <p>Рекомендуется вызывать в начале каждого теста для гарантии
     * корректной загрузки всех необходимых компонентов.
     *
     * <p>Пример использования:
     * <pre>{@code
     * @Test
     * void testServiceMethod() {
     *     assertAllBeansLoaded();
     *     // Дальнейшие проверки
     * }
     * }</pre>
     *
     * @throws AssertionError если любой из бинов не загружен
     */
    protected void assertAllBeansLoaded() {
        assertApplicationContextLoaded();
        assertMocksCreated();
        logger.info("All beans loaded successfully");
    }

    /**
     * Возвращает имя класса теста для использования в логировании.
     *
     * <p>Упрощает идентификацию теста в логах, особенно при параллельном
     * выполнении нескольких тестов.
     *
     * <p>Пример использования:
     * <pre>{@code
     * logger.info("Test {} is starting", getTestName());
     * // или
     * logger.info("Test {} completed successfully", getTestName());
     * }</pre>
     *
     * @return простое имя класса теста (без имени пакета)
     */
    protected String getTestName() {
        return getClass().getSimpleName();
    }

    /**
     * Возвращает полное имя класса теста для детального логирования.
     *
     * <p>Используется для более подробного логирования, когда
     * необходимо знать полный путь к классу.
     *
     * @return полное имя класса теста (с именем пакета)
     */
    protected String getFullTestName() {
        return getClass().getName();
    }

    /**
     * Логирует начало выполнения теста с дополнительной информацией.
     *
     * <p>Автоматически вызывается в {@link #setUpBase()}, но может быть
     * вызван вручную для дополнительного логирования.
     *
     * @param message дополнительное сообщение для логирования
     */
    protected void logTestStart(String message) {
        logger.info("🚀 [{}] {}", getTestName(), message);
    }

    /**
     * Логирует успешное завершение теста.
     *
     * @param message дополнительное сообщение для логирования
     */
    protected void logTestSuccess(String message) {
        logger.info("✅ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует предупреждение в тесте.
     *
     * @param message сообщение предупреждения
     */
    protected void logTestWarning(String message) {
        logger.warn("⚠️ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует ошибку в тесте.
     *
     * @param message   сообщение об ошибке
     * @param throwable исключение для логирования
     */
    protected void logTestError(String message, Throwable throwable) {
        logger.error("❌ [{}] {}", getTestName(), message, throwable);
    }
}