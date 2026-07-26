package com.example.rag.config;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import com.example.rag.support.BaseTest;
import com.example.rag.support.TestUtils;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки конфигурации базы данных.
 * <p>
 * Проверяет:
 * <ul>
 *   <li>Наличие и доступность DataSource</li>
 *   <li>Установление соединения с базой данных</li>
 *   <li>Выполнение простых SQL запросов</li>
 *   <li>Проверка, что используется H2 для модульных тестов</li>
 * </ul>
 * <p>
 * <b>Важно:</b> Для модульных тестов используется H2, поэтому pgvector НЕ проверяется.
 * Проверка pgvector вынесена в интеграционные тесты (BaseIntegrationTest).
 *
 * @author RAG Application Team
 * @version 6.1
 * @see BaseTest
 * @see TestUtils
 * @since 1.0
 */
@Slf4j
@Epic("Модульные тесты")
@Feature("Конфигурация базы данных")
class DatabaseConfigurationTest extends BaseTest {

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private Environment environment;

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    /**
     * Проверяет, что DataSource доступен и подключение работает.
     *
     * @throws SQLException если ошибка подключения к базе данных
     */
    @Test
    @Description("Проверка доступности DataSource")
    @Story("Подключение к БД")
    @Severity(SeverityLevel.CRITICAL)
    void testDataSource() throws SQLException {
        logTestStart("Testing DataSource availability");

        assertMocksCreated();
        assertDataSourceAvailable();

        // Измеряем время подключения
        TestUtils.measureExecutionTime("Database connection", () -> {
            try {
                assertDataSourceAvailable();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        verifyH2Driver();
        log.info("✅ DataSource configured correctly");
        logTestSuccess("DataSource availability verified");
    }

    /**
     * Проверяет, что DataSource правильно сконфигурирован.
     *
     * @throws SQLException если ошибка подключения к базе данных
     */
    @Test
    @Description("Проверка конфигурации DataSource")
    @Story("Конфигурация DataSource")
    @Severity(SeverityLevel.CRITICAL)
    void testDataSourceIsConfigured() throws SQLException {
        logTestStart("Testing DataSource configuration");

        assertMocksCreated();
        assertDataSourceAvailable();
        verifyH2Driver();

        log.info("✅ DataSource configuration verified");
        logTestSuccess("DataSource configuration verified");
    }

    /**
     * Проверяет выполнение SQL запроса.
     * <p>
     * <b>Примечание:</b> Проверка pgvector выполняется только в интеграционных тестах,
     * так как H2 не поддерживает расширения PostgreSQL.
     *
     * @throws SQLException если ошибка выполнения запроса
     */
    @Test
    @Description("Проверка выполнения SQL запроса")
    @Story("SQL запросы")
    @Severity(SeverityLevel.CRITICAL)
    void testDatabaseConnection() throws SQLException {
        logTestStart("Testing database connection");

        assertMocksCreated();

        try (var connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5))
                    .as("Connection should be valid")
                    .isTrue();

            // Выполняем простой запрос
            executeSimpleQuery(connection);

            log.info("✅ Database query executed successfully (H2 in-memory)");
        } catch (SQLException e) {
            log.error("❌ Database connection failed: {}", e.getMessage());
            throw e;
        }

        logTestSuccess("Database connection verified");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Выполняет простой SQL запрос для проверки соединения.
     *
     * @param connection подключение к базе данных
     * @throws SQLException если ошибка выполнения запроса
     */
    private void executeSimpleQuery(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT 1")) {

            assertThat(resultSet.next())
                    .as("Query should return result")
                    .isTrue();

            assertThat(resultSet.getInt(1))
                    .as("Result should be 1")
                    .isEqualTo(1);

            log.debug("   ✅ Simple query executed successfully");
        }
    }

    /**
     * Проверяет, что используется H2 драйвер для модульных тестов.
     */
    private void verifyH2Driver() {
        var driver = environment.getProperty("spring.datasource.driver-class-name");
        var url = environment.getProperty("spring.datasource.url");

        // Для модульных тестов используем H2
        assertThat(driver)
                .as("Driver should be H2 for unit tests")
                .isEqualTo("org.h2.Driver");

        assertThat(url)
                .as("URL should be H2 for unit tests")
                .contains("h2")
                .doesNotContain("postgresql");

        log.debug("   Driver: {}", driver);
        log.debug("   URL: {}", url);
    }

    /**
     * Проверяет, что pgvector расширение установлено.
     * <p>
     * <b>Важно:</b> Этот метод используется ТОЛЬКО в интеграционных тестах с PostgreSQL.
     * Для модульных тестов с H2 эта проверка пропускается.
     *
     * @param connection подключение к базе данных
     * @throws SQLException если ошибка выполнения запроса
     */
    private void verifyPgvectorExtension(Connection connection) throws SQLException {
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT extname FROM pg_extension WHERE extname = 'vector'")) {

            assertThat(rs.next())
                    .as("pgvector extension should be installed")
                    .isTrue();

            log.debug("   ✅ pgvector extension installed");
        }
    }
}