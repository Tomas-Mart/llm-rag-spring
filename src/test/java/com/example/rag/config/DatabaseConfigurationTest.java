package com.example.rag.config;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import com.example.rag.support.BaseTest;
import com.example.rag.support.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки конфигурации базы данных.
 * Проверяет подключение к DataSource и базовые операции.
 *
 * <p>Тестируемые аспекты:
 * <ul>
 *   <li>Наличие и доступность DataSource</li>
 *   <li>Установление соединения с базой данных</li>
 *   <li>Выполнение простых SQL запросов</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
class DatabaseConfigurationTest extends BaseTest {

    /**
     * Проверяет, что все моки созданы.
     */
    @Test
    void testMocksAreCreated() {
        assertMocksCreated();
        logger.info("All mocks created successfully");
    }

    /**
     * Проверяет, что DataSource доступен и подключение к базе данных работает.
     *
     * <p>Измеряет время подключения к базе данных с помощью {@link TestUtils}.
     *
     * @throws SQLException если ошибка подключения к базе данных
     */
    @Test
    void testDataSource() throws SQLException {
        assertMocksCreated();
        assertDataSourceAvailable();

        TestUtils.measureExecutionTime("Database connection", () -> {
            try {
                assertDataSourceAvailable();
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        });

        assertThat(dataSource)
                .as("DataSource should be available")
                .isNotNull();

        logger.info("DataSource configured correctly");
    }

    /**
     * Проверяет, что DataSource правильно сконфигурирован.
     *
     * @throws SQLException если ошибка подключения к базе данных
     */
    @Test
    void testDataSourceIsConfigured() throws SQLException {
        assertMocksCreated();
        assertDataSourceAvailable();
        logger.info("DataSource configuration verified");
    }

    /**
     * Проверяет выполнение простого SQL запроса.
     *
     * <p>Выполняет запрос {@code SELECT 1} и проверяет результат.
     *
     * @throws SQLException если ошибка выполнения запроса
     */
    @Test
    void testDatabaseConnection() throws SQLException {
        assertMocksCreated();

        try (var connection = dataSource.getConnection()) {
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery("SELECT 1");

            assertThat(resultSet.next())
                    .as("Should execute query successfully")
                    .isTrue();
            assertThat(resultSet.getInt(1))
                    .as("Should return 1")
                    .isEqualTo(1);

            logger.info("Database query executed successfully");
        }
    }
}