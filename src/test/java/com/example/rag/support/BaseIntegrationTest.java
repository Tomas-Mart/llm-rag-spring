package com.example.rag.support;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.example.rag.Application;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Абстрактный базовый класс для всех интеграционных тестов приложения.
 * Использует реальную PostgreSQL с pgvector из конфигурации.
 *
 * <h2>Основные возможности</h2>
 * <ul>
 *   <li>Загрузка Spring контекста с профилем {@code integration-test}</li>
 *   <li>Моки для Ollama API и Chat Model</li>
 *   <li>Реальный VectorStore для проверки эмбеддингов</li>
 *   <li>Flyway для управления схемой базы данных</li>
 * </ul>
 *
 * <h2>Важно</h2>
 * <p>В этом классе НЕТ H2 и Testcontainers. Используется существующий контейнер PostgreSQL.</p>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * @SpringBootTest
 * @ActiveProfiles("integration-test")
 * class DocumentIngestionServiceIntegrationTest extends BaseIntegrationTest {
 *
 *     @Autowired
 *     private DocumentIngestionService ingestionService;
 *
 *     @Test
 *     void testIngestDocument() throws Exception {
 *         ingestionService.ingestDocument(file, "test");
 *         var saved = documentRepository.findAll();
 *         assertThat(saved).isNotEmpty();
 *     }
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 6.0
 * @see BaseTest
 * @see BaseIntegrationTestWithContainers
 * @since 1.0
 */
@Slf4j
@Transactional
@Tag("integration")
@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    // ============================================================
    // SPRING BEANS
    // ============================================================

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected Environment environment;

    @Autowired
    protected Application application;

    @MockBean
    protected OllamaApi ollamaApi;

    @MockBean
    protected OllamaChatModel ollamaChatModel;

    @Autowired(required = false)
    protected VectorStore vectorStore;

    @Autowired(required = false)
    protected DataSource dataSource;

    // ============================================================
    // SQL КОНСТАНТЫ (Java 21 text blocks)
    // ============================================================

    private static final String CHECK_EXTENSION_SQL = """
            SELECT extname FROM pg_extension WHERE extname = 'vector'
            """;

    private static final String CHECK_VECTOR_STORE_TABLE_SQL = """
            SELECT EXISTS (
                SELECT 1 FROM information_schema.tables
                WHERE table_name = 'vector_store'
                AND table_schema = 'public'
            )
            """;

    private static final String CHECK_VECTOR_TYPE_SQL = """
            SELECT EXISTS (
                SELECT 1 FROM pg_type
                WHERE typname = 'vector'
            )
            """;

    private static final String TEST_VECTOR_SQL = """
            SELECT array[1.0, 2.0, 3.0]::vector
            """;

    private static final String CHECK_FLYWAY_SQL = """
            SELECT version, description, success
            FROM flyway_schema_history
            ORDER BY installed_rank DESC
            LIMIT 1
            """;

    private static final String COUNT_VECTORS_SQL = """
            SELECT COUNT(*) FROM vector_store
            """;

    private static final String CHECK_DOCUMENTS_TABLE_SQL = """
            SELECT EXISTS (
                SELECT 1 FROM information_schema.tables
                WHERE table_name = 'documents'
                AND table_schema = 'public'
            )
            """;

    private static final String TRUNCATE_VECTOR_STORE_SQL = """
            TRUNCATE TABLE vector_store CASCADE
            """;

    private static final String TRUNCATE_DOCUMENT_SQL = """
            TRUNCATE TABLE document CASCADE
            """;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUpBase() throws SQLException {
        log.info("🚀 Running integration test: {}", getTestName());
        log.debug("📋 Test class: {}", getClass().getName());
        log.debug("🐘 PostgreSQL URL: {}", environment.getProperty("spring.datasource.url"));
        log.debug("📊 Active profiles: {}", String.join(", ", environment.getActiveProfiles()));

        assertAllComponentsLoaded();
    }

    // ============================================================
    // ПРОВЕРКИ КОМПОНЕНТОВ
    // ============================================================

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

    protected void assertVectorStoreAvailable() {
        assertThat(vectorStore)
                .as("VectorStore should be available for integration tests")
                .isNotNull();

        log.info("✅ VectorStore is available");
        log.debug("   - VectorStore type: {}", vectorStore.getClass().getSimpleName());
    }

    protected void assertDataSourceAvailable() throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available")
                .isNotNull();

        try (var connection = dataSource.getConnection()) {
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

    protected void assertPgvectorAvailable() throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available for pgvector check")
                .isNotNull();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            // 1. Проверяем установку расширения pgvector
            try (var rs = stmt.executeQuery(CHECK_EXTENSION_SQL)) {
                boolean hasExtension = rs.next();
                assertThat(hasExtension)
                        .as("pgvector extension should be installed")
                        .isTrue();
                log.info("✅ pgvector extension is installed");
            }

            // 2. Проверяем существование таблицы vector_store
            try (var rs = stmt.executeQuery(CHECK_VECTOR_STORE_TABLE_SQL)) {
                rs.next();
                boolean tableExists = rs.getBoolean(1);
                assertThat(tableExists)
                        .as("vector_store table should exist")
                        .isTrue();
                log.info("✅ vector_store table exists");
            }

            // 3. Проверяем тип vector
            try (var rs = stmt.executeQuery(CHECK_VECTOR_TYPE_SQL)) {
                rs.next();
                boolean vectorTypeExists = rs.getBoolean(1);
                assertThat(vectorTypeExists)
                        .as("vector type should exist")
                        .isTrue();
                log.info("✅ vector type exists");
            }

            // 4. Тестируем создание вектора
            try (var rs = stmt.executeQuery(TEST_VECTOR_SQL)) {
                rs.next();
                var vector = rs.getString(1);
                assertThat(vector)
                        .as("Vector type should work")
                        .contains("1", "2", "3");
                log.info("✅ vector type is working: {}", vector);
            }

        } catch (SQLException e) {
            log.error("❌ pgvector validation failed", e);
            throw e;
        }
    }

    protected void assertFlywayMigrationsApplied() throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available for Flyway check")
                .isNotNull();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(CHECK_FLYWAY_SQL)) {

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

    protected void assertDoesNotThrow(Runnable code, String message) {
        assertThatCode(code::run)
                .as(message)
                .doesNotThrowAnyException();
    }

    protected void cleanDatabase() throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available for cleanup")
                .isNotNull();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(TRUNCATE_VECTOR_STORE_SQL);
            stmt.execute(TRUNCATE_DOCUMENT_SQL);
            log.info("🧹 Database cleaned up successfully");
        } catch (SQLException e) {
            log.error("❌ Failed to clean database", e);
            throw e;
        }
    }

    protected void assertVectorCount(long expectedCount) throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available for count check")
                .isNotNull();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(COUNT_VECTORS_SQL)) {
            rs.next();
            long actualCount = rs.getLong(1);
            assertThat(actualCount)
                    .as("Vector store should contain %d records", expectedCount)
                    .isEqualTo(expectedCount);
            log.info("✅ Vector store contains {} records", actualCount);
        }
    }

    protected void assertDocumentTableExists() throws SQLException {
        assertThat(dataSource)
                .as("DataSource should be available for table check")
                .isNotNull();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(CHECK_DOCUMENTS_TABLE_SQL)) {
            rs.next();
            boolean tableExists = rs.getBoolean(1);
            assertThat(tableExists)
                    .as("documents table should exist")
                    .isTrue();
            log.info("✅ documents table exists");
        }
    }
}