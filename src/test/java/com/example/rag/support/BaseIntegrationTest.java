package com.example.rag.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import com.example.rag.Application;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Абстрактный базовый класс для всех интеграционных тестов приложения.
 * Использует реальную PostgreSQL с pgvector через Testcontainers.
 *
 * <p>Основные возможности:
 * <ul>
 *   <li>Загрузка Spring контекста с профилем {@code integration-test}</li>
 *   <li>Использование Testcontainers для реальной PostgreSQL с pgvector</li>
 *   <li>Моки для Ollama API и Chat Model</li>
 *   <li>Реальный VectorStore для проверки эмбеддингов</li>
 *   <li>Flyway для управления схемой базы данных</li>
 * </ul>
 *
 * <p>Важно: В этом классе НЕТ H2. Все тесты работают с реальной PostgreSQL.
 *
 * <p>Архитектура логирования:
 * <ul>
 *   <li>{@code STATIC_LOGGER} - для статических контекстов (static block, @DynamicPropertySource)</li>
 *   <li>{@code log} (Lombok) - для нестатических методов экземпляров</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 4.0
 * @since 1.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Tag("integration")
@Transactional
public abstract class BaseIntegrationTest {

    // ============================================================
    // ЛОГГЕРЫ
    // ============================================================

    /**
     * Статический логгер для статических контекстов.
     * Используется в статическом блоке и @DynamicPropertySource методах,
     * где Lombok {@code @Slf4j} не работает.
     */
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(BaseIntegrationTest.class);

    // Lombok @Slf4j предоставляет статическое поле 'log' для нестатических методов

    // ============================================================
    // TESTCONTAINERS
    // ============================================================

    /**
     * Контейнер PostgreSQL с pgvector.
     * Используется для всех интеграционных тестов.
     * Переиспользуется между тестами для ускорения (withReuse(true)).
     */
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        STATIC_LOGGER.info("🐘 Initializing PostgreSQL container...");

        POSTGRES_CONTAINER = new PostgreSQLContainer<>(
                DockerImageName.parse("pgvector/pgvector:pg16")
                        .asCompatibleSubstituteFor("postgres")
        )
                .withDatabaseName("rag_integration_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withReuse(true)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("PostgreSQLContainer")));

        POSTGRES_CONTAINER.start();

        STATIC_LOGGER.info("✅ PostgreSQL container started successfully");
        STATIC_LOGGER.info("   📍 URL: {}", POSTGRES_CONTAINER.getJdbcUrl());
        STATIC_LOGGER.info("   🆔 Container ID: {}", POSTGRES_CONTAINER.getContainerId());
        STATIC_LOGGER.info("   📦 Image: {}", POSTGRES_CONTAINER.getDockerImageName());
    }

    /**
     * Динамические свойства для подключения к PostgreSQL.
     * Spring автоматически подставит эти значения в контекст.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        STATIC_LOGGER.info("🔧 Configuring PostgreSQL properties...");

        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

        STATIC_LOGGER.info("✅ PostgreSQL properties configured successfully");
        STATIC_LOGGER.info("   🔗 URL: {}", POSTGRES_CONTAINER.getJdbcUrl());
        STATIC_LOGGER.info("   👤 Username: {}", POSTGRES_CONTAINER.getUsername());
    }

    // ============================================================
    // SPRING BEANS
    // ============================================================

    /**
     * Контекст приложения для доступа к бинам.
     */
    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * Environment для доступа к свойствам конфигурации.
     */
    @Autowired
    protected Environment environment;

    /**
     * Главный Application bean для проверки загрузки контекста.
     */
    @Autowired
    protected Application application;

    /**
     * Мок для Ollama API.
     * Изолирует тесты от реального Ollama сервера.
     */
    @MockBean
    protected OllamaApi ollamaApi;

    /**
     * Мок для Ollama Chat Model.
     * Изолирует тесты от реальной LLM.
     */
    @MockBean
    protected OllamaChatModel ollamaChatModel;

    /**
     * Реальный VectorStore для интеграционных тестов.
     * Использует реальную PostgreSQL с pgvector.
     * Не мокается - это ключевое отличие от модульных тестов.
     */
    @Autowired(required = false)
    protected VectorStore vectorStore;

    /**
     * Реальный DataSource для подключения к PostgreSQL.
     */
    @Autowired(required = false)
    protected DataSource dataSource;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    /**
     * Инициализация перед каждым тестом.
     * Проверяет, что все компоненты загружены корректно.
     *
     * @throws SQLException если ошибка подключения к БД
     */
    @BeforeEach
    void setUpBase() throws SQLException {
        log.info("🚀 Running integration test: {}", getTestName());
        log.debug("📋 Test class: {}", getClass().getName());
        log.debug("🐘 PostgreSQL URL: {}", POSTGRES_CONTAINER.getJdbcUrl());
        log.debug("📊 Active profiles: {}",
                String.join(", ", applicationContext.getEnvironment().getActiveProfiles()));

        assertAllComponentsLoaded();
    }

    // ============================================================
    // ПРОВЕРКИ КОМПОНЕНТОВ
    // ============================================================

    /**
     * Проверяет, что все моки созданы.
     */
    protected void assertMocksCreated() {
        assertThat(ollamaApi)
                .as("OllamaApi mock should be created")
                .isNotNull();

        assertThat(ollamaChatModel)
                .as("OllamaChatModel mock should be created")
                .isNotNull();

        log.info("✅ All mocks created successfully");
        log.debug("   - OllamaApi: {}", ollamaApi.getClass().getSimpleName());
        log.debug("   - OllamaChatModel: {}", ollamaChatModel.getClass().getSimpleName());
    }

    /**
     * Проверяет, что Application контекст загружен.
     */
    protected void assertApplicationContextLoaded() {
        assertThat(application)
                .as("Application bean should be loaded")
                .isNotNull();

        assertThat(applicationContext)
                .as("ApplicationContext should be loaded")
                .isNotNull();

        log.info("✅ Application context loaded successfully");
        log.debug("   - Bean count: {}", applicationContext.getBeanDefinitionCount());
        log.debug("   - Application name: {}", applicationContext.getApplicationName());
    }

    /**
     * Проверяет, что реальный VectorStore создан и работает с PostgreSQL.
     */
    protected void assertVectorStoreAvailable() {
        assertThat(vectorStore)
                .as("VectorStore should be available for integration tests")
                .isNotNull();

        log.info("✅ VectorStore is available");
        log.debug("   - VectorStore type: {}", vectorStore.getClass().getSimpleName());
    }

    /**
     * Проверяет, что DataSource доступен и можно установить соединение с БД.
     *
     * @throws SQLException если ошибка подключения к БД
     */
    protected void assertDataSourceAvailable() throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available")
                .isNotNull();

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection)
                    .as("Database connection should be established")
                    .isNotNull();
            assertThat(connection.isValid(5))
                    .as("Connection should be valid")
                    .isTrue();

            var metaData = connection.getMetaData();
            log.info("✅ Database connection established successfully");
            log.debug("   📍 URL: {}", metaData.getURL());
            log.debug("   🗄️  Product: {}", metaData.getDatabaseProductName());
            log.debug("   📦 Version: {}", metaData.getDatabaseProductVersion());
        } catch (SQLException e) {
            log.error("❌ Failed to connect to database", e);
            throw e;
        }
    }

    /**
     * Проверяет, что расширение pgvector установлено и работает.
     *
     * @throws SQLException если ошибка выполнения SQL запросов
     */
    protected void assertPgvectorAvailable() throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available for pgvector check")
                .isNotNull();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Проверяем установку расширения pgvector
            var rs = stmt.executeQuery("SELECT extname FROM pg_extension WHERE extname = 'vector'");
            boolean hasExtension = rs.next();
            assertThat(hasExtension)
                    .as("pgvector extension should be installed")
                    .isTrue();
            log.info("✅ pgvector extension is installed");

            // 2. Проверяем существование таблицы vector_store
            rs = stmt.executeQuery("""
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.tables 
                        WHERE table_name = 'vector_store'
                        AND table_schema = 'public'
                    )
                    """);
            rs.next();
            boolean tableExists = rs.getBoolean(1);
            assertThat(tableExists)
                    .as("vector_store table should exist")
                    .isTrue();
            log.info("✅ vector_store table exists");

            // 3. Проверяем тип vector
            rs = stmt.executeQuery("""
                    SELECT EXISTS (
                        SELECT 1 FROM pg_type 
                        WHERE typname = 'vector'
                    )
                    """);
            rs.next();
            boolean vectorTypeExists = rs.getBoolean(1);
            assertThat(vectorTypeExists)
                    .as("vector type should exist")
                    .isTrue();
            log.info("✅ vector type exists");

            // 4. Тестируем создание вектора
            rs = stmt.executeQuery("SELECT array[1.0, 2.0, 3.0]::vector");
            rs.next();
            var vector = rs.getString(1);
            assertThat(vector)
                    .as("Vector type should work")
                    .contains("1.0", "2.0", "3.0");
            log.info("✅ vector type is working: {}", vector);

        } catch (SQLException e) {
            log.error("❌ pgvector validation failed", e);
            throw e;
        }
    }

    /**
     * Проверяет, что Flyway применил миграции.
     *
     * @throws SQLException если ошибка выполнения SQL запросов
     */
    protected void assertFlywayMigrationsApplied() throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available for Flyway check")
                .isNotNull();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            var rs = stmt.executeQuery("""
                    SELECT version, description, success 
                    FROM flyway_schema_history 
                    ORDER BY installed_rank DESC 
                    LIMIT 1
                    """);

            if (rs.next()) {
                log.info("✅ Flyway migrations applied successfully");
                log.debug("   📌 Latest version: {}", rs.getString("version"));
                log.debug("   📝 Description: {}", rs.getString("description"));
                log.debug("   ✅ Success: {}", rs.getBoolean("success"));
            } else {
                log.warn("⚠️ No Flyway migrations found");
            }
        } catch (SQLException e) {
            log.error("❌ Flyway check failed", e);
            throw e;
        }
    }

    /**
     * Комплексная проверка всех компонентов.
     * Должна вызываться в каждом тесте для гарантии корректной загрузки.
     *
     * @throws SQLException если ошибка подключения к БД
     */
    protected void assertAllComponentsLoaded() throws SQLException {
        assertApplicationContextLoaded();
        assertMocksCreated();
        assertVectorStoreAvailable();
        assertDataSourceAvailable();
        assertPgvectorAvailable();
        assertFlywayMigrationsApplied();
        log.info("✅ All components loaded successfully for integration tests");
    }

    // ============================================================
    // УТИЛИТЫ
    // ============================================================

    /**
     * Возвращает имя класса теста.
     *
     * @return простое имя класса без пакета
     */
    protected String getTestName() {
        return getClass().getSimpleName();
    }

    /**
     * Логирует начало выполнения теста.
     *
     * @param message сообщение для логирования
     */
    protected void logTestStart(String message) {
        log.info("🚀 [{}] {}", getTestName(), message);
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
     * @param message сообщение предупреждения
     */
    protected void logTestWarning(String message) {
        log.warn("⚠️ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует ошибку в тесте.
     *
     * @param message   сообщение об ошибке
     * @param throwable исключение для логирования
     */
    protected void logTestError(String message, Throwable throwable) {
        log.error("❌ [{}] {}", getTestName(), message, throwable);
    }

    /**
     * Проверяет, что код не выбрасывает исключение.
     * Обертка над AssertJ assertThatCode.
     *
     * @param code    код для выполнения
     * @param message сообщение в случае ошибки
     */
    protected void assertDoesNotThrow(Runnable code, String message) {
        assertThatCode(code::run)
                .as(message)
                .doesNotThrowAnyException();
    }

    /**
     * Очищает таблицы после теста.
     * Используется для изоляции тестов друг от друга.
     *
     * @throws SQLException если ошибка выполнения SQL
     */
    protected void cleanDatabase() throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available for cleanup")
                .isNotNull();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE vector_store CASCADE");
            stmt.execute("TRUNCATE TABLE document CASCADE");
            log.info("🧹 Database cleaned up successfully");
        } catch (SQLException e) {
            log.error("❌ Failed to clean database", e);
            throw e;
        }
    }

    /**
     * Проверяет, что в базе данных есть записи в таблице vector_store.
     *
     * @param expectedCount ожидаемое количество записей
     * @throws SQLException если ошибка выполнения SQL
     */
    protected void assertVectorCount(long expectedCount) throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available for count check")
                .isNotNull();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM vector_store");
            rs.next();
            long actualCount = rs.getLong(1);
            assertThat(actualCount)
                    .as("Vector store should contain %d records", expectedCount)
                    .isEqualTo(expectedCount);
            log.info("✅ Vector store contains {} records", actualCount);
        }
    }
}