package com.example.rag.config;

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
 *
 * <h2>Назначение</h2>
 * <p>Проверяет подключение к DataSource и базовые операции с БД.</p>
 *
 * <h2>Тестируемые аспекты</h2>
 * <ul>
 *   <li>Наличие и доступность DataSource</li>
 *   <li>Установление соединения с базой данных</li>
 *   <li>Выполнение простых SQL запросов</li>
 *   <li>Проверка, что используется PostgreSQL, а не H2</li>
 * </ul>
 *
 * <h2>Пример запуска</h2>
 * <pre>{@code
 * // Запустить все тесты
 * mvn test -Dtest=DatabaseConfigurationTest
 *
 * // Запустить конкретный тест
 * mvn test -Dtest=DatabaseConfigurationTest#testDatabaseConnection
 * }</pre>
 *
 * @author RAG Application Team
 * @version 5.0
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

    @Test
    @Description("Проверка, что DataSource доступен и подключение работает")
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

        assertThat(dataSource)
                .as("DataSource should be available")
                .isNotNull();

        // Проверяем, что используется PostgreSQL
        verifyPostgreSqlDriver();

        log.info("✅ DataSource configured correctly");
        logTestSuccess("DataSource availability verified");
    }

    @Test
    @Description("Проверка, что DataSource правильно сконфигурирован")
    @Story("Конфигурация DataSource")
    @Severity(SeverityLevel.CRITICAL)
    void testDataSourceIsConfigured() throws SQLException {
        logTestStart("Testing DataSource configuration");

        assertMocksCreated();
        assertDataSourceAvailable();

        verifyPostgreSqlDriver();

        log.info("✅ DataSource configuration verified");
        logTestSuccess("DataSource configuration verified");
    }

    @Test
    @Description("Проверка выполнения простого SQL запроса")
    @Story("SQL запросы")
    @Severity(SeverityLevel.CRITICAL)
    void testDatabaseConnection() throws SQLException {
        logTestStart("Testing database connection");

        assertMocksCreated();

        try (var connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5))
                    .as("Connection should be valid")
                    .isTrue();

            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT 1")) {

                assertThat(resultSet.next())
                        .as("Should execute query successfully")
                        .isTrue();

                assertThat(resultSet.getInt(1))
                        .as("Should return 1")
                        .isEqualTo(1);
            }

            // Проверяем pgvector
            verifyPgvectorExtension(connection);

            log.info("✅ Database query executed successfully");
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
     * Проверяет, что используется PostgreSQL драйвер.
     */
    private void verifyPostgreSqlDriver() {
        String driver = environment.getProperty("spring.datasource.driver-class-name");
        assertThat(driver)
                .as("Driver should be PostgreSQL")
                .isEqualTo("org.postgresql.Driver");

        String url = environment.getProperty("spring.datasource.url");
        assertThat(url)
                .as("URL should be PostgreSQL")
                .contains("postgresql")
                .doesNotContain("h2");

        log.debug("   Driver: {}", driver);
        log.debug("   URL: {}", url);
    }

    /**
     * Проверяет, что pgvector установлен.
     *
     * @param connection подключение к БД
     * @throws SQLException если ошибка выполнения запроса
     */
    private void verifyPgvectorExtension(java.sql.Connection connection) throws SQLException {
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT extname FROM pg_extension WHERE extname = 'vector'")) {

            assertThat(rs.next())
                    .as("pgvector extension should be installed")
                    .isTrue();

            log.debug("   ✅ pgvector extension installed");
        }
    }
}