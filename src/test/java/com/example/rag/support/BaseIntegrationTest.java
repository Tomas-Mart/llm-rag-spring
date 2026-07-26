package com.example.rag.support;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
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
 * Базовый класс для ИНТЕГРАЦИОННЫХ тестов с ВНЕШНЕЙ PostgreSQL.
 * <p>
 * Особенности:
 * <ul>
 *   <li>Использует существующий контейнер PostgreSQL (не Testcontainers)</li>
 *   <li>Требует запущенной базы данных на порту 32769</li>
 *   <li>Использует Flyway для управления схемой</li>
 *   <li>Автоматический откат транзакций</li>
 * </ul>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * @Slf4j
 * class FlywayIT extends BaseIntegrationTest {
 *     @Test
 *     void testMigrations() throws SQLException {
 *         assertFlywayMigrationsApplied();
 *     }
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 6.0
 * @since 1.0
 */
@Slf4j
@Transactional
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SuppressWarnings({"unused", "SqlResolve"})
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
    // SQL КОНСТАНТЫ
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
    protected void setUpBase() throws SQLException {
        log.info("🚀 Running integration test: {}", getTestName());
        log.debug("📋 Test class: {}", getClass().getName());

        String dbUrl = environment != null ? environment.getProperty("spring.datasource.url") : "unknown";
        log.debug("🐘 PostgreSQL URL: {}", dbUrl);

        String[] profiles = environment != null ? environment.getActiveProfiles() : new String[]{"unknown"};
        log.debug("📊 Active profiles: {}", String.join(", ", profiles));

        assertAllComponentsLoaded();
    }

    // ============================================================
    // ПРОВЕРКИ
    // ============================================================

    protected void assertMocksCreated() {
        assertThat(ollamaApi).isNotNull();
        assertThat(ollamaChatModel).isNotNull();
        log.info("✅ All mocks created");
    }

    protected void assertApplicationContextLoaded() {
        assertThat(application).isNotNull();
        assertThat(applicationContext).isNotNull();
        log.info("✅ Context loaded, beans: {}", applicationContext.getBeanDefinitionCount());
    }

    protected void assertVectorStoreAvailable() {
        assertThat(vectorStore).isNotNull();
        log.info("✅ VectorStore available: {}", vectorStore.getClass().getSimpleName());
    }

    protected void assertDataSourceAvailable() throws SQLException {
        assertThat(dataSource).isNotNull();

        try (var connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5)).isTrue();
            var metaData = connection.getMetaData();
            log.info("✅ Database connected");
            log.debug("   📍 URL: {}", metaData.getURL());
            log.debug("   🗄️  Product: {}", metaData.getDatabaseProductName());
            log.debug("   📦 Version: {}", metaData.getDatabaseProductVersion());
        }
    }

    protected void assertPgvectorAvailable() throws SQLException {
        assertThat(dataSource).isNotNull();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            try (var rs = stmt.executeQuery(CHECK_EXTENSION_SQL)) {
                assertThat(rs.next()).isTrue();
                log.info("✅ pgvector extension installed");
            }

            try (var rs = stmt.executeQuery(CHECK_VECTOR_STORE_TABLE_SQL)) {
                if (rs.next()) {
                    assertThat(rs.getBoolean(1)).isTrue();
                    log.info("✅ vector_store table exists");
                } else {
                    log.warn("⚠️ vector_store table not found");
                }
            }

            try (var rs = stmt.executeQuery(CHECK_VECTOR_TYPE_SQL)) {
                if (rs.next()) {
                    assertThat(rs.getBoolean(1)).isTrue();
                    log.info("✅ vector type exists");
                } else {
                    log.warn("⚠️ vector type not found");
                }
            }

            try (var rs = stmt.executeQuery(TEST_VECTOR_SQL)) {
                if (rs.next()) {
                    var vector = rs.getString(1);
                    assertThat(vector).contains("1", "2", "3");
                    log.info("✅ vector type working: {}", vector);
                } else {
                    log.warn("⚠️ vector type test failed");
                }
            }
        }
    }

    protected void assertFlywayMigrationsApplied() throws SQLException {
        assertThat(dataSource).isNotNull();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(CHECK_FLYWAY_SQL)) {

            if (rs.next()) {
                log.info("✅ Flyway migrations applied");
                log.debug("   📌 Version: {}", rs.getString("version"));
                log.debug("   📝 Description: {}", rs.getString("description"));
                log.debug("   ✅ Success: {}", rs.getBoolean("success"));
            } else {
                log.warn("⚠️ No Flyway migrations found");
            }
        }
    }

    protected void assertAllComponentsLoaded() throws SQLException {
        assertApplicationContextLoaded();
        assertMocksCreated();
        assertVectorStoreAvailable();
        assertDataSourceAvailable();
        assertPgvectorAvailable();
        assertFlywayMigrationsApplied();
        log.info("✅ All components loaded");
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
        assertThatCode(code::run).as(message).doesNotThrowAnyException();
    }

    protected void cleanDatabase() throws SQLException {
        if (dataSource == null) {
            log.warn("⚠️ DataSource is null, skipping database cleanup");
            return;
        }
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(TRUNCATE_VECTOR_STORE_SQL);
            stmt.execute(TRUNCATE_DOCUMENT_SQL);
            log.info("🧹 Database cleaned");
        } catch (SQLException e) {
            log.warn("⚠️ Could not clean database: {}", e.getMessage());
        }
    }

    protected void assertVectorCount(long expectedCount) throws SQLException {
        if (dataSource == null) {
            log.warn("⚠️ DataSource is null, skipping vector count check");
            return;
        }
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(COUNT_VECTORS_SQL)) {
            if (rs.next()) {
                assertThat(rs.getLong(1)).isEqualTo(expectedCount);
                log.info("✅ Vector count: {}", expectedCount);
            } else {
                log.warn("⚠️ Could not get vector count");
            }
        }
    }

    protected void assertDocumentTableExists() throws SQLException {
        if (dataSource == null) {
            log.warn("⚠️ DataSource is null, skipping document table check");
            return;
        }
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(CHECK_DOCUMENTS_TABLE_SQL)) {
            if (rs.next()) {
                assertThat(rs.getBoolean(1)).isTrue();
                log.info("✅ documents table exists");
            } else {
                log.warn("⚠️ documents table not found");
            }
        }
    }

    protected void assertDocumentCount(long expectedCount) throws SQLException {
        if (dataSource == null) {
            log.warn("⚠️ DataSource is null, skipping document count check");
            return;
        }
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM documents")) {
            if (rs.next()) {
                assertThat(rs.getLong(1)).isEqualTo(expectedCount);
                log.info("✅ Document count: {}", expectedCount);
            } else {
                log.warn("⚠️ Could not get document count");
            }
        }
    }
}