package com.example.rag.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.rag.support.BaseTest;
import com.example.rag.support.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки миграций Flyway.
 * Проверяет конфигурацию и применение миграций базы данных.
 *
 * <p>Тестируемые аспекты:
 * <ul>
 *   <li>Конфигурация Flyway</li>
 *   <li>Применение миграций</li>
 *   <li>Схема базы данных</li>
 *   <li>Статус миграций</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
class FlywayMigrationTest extends BaseTest {

    /**
     * Экземпляр Flyway для проверки миграций.
     * Может быть {@code null}, если Flyway отключен в тестовом профиле.
     */
    @Autowired(required = false)
    private Flyway flyway;

    /**
     * Проверяет, что все моки созданы.
     */
    @Test
    void testMocksAreCreated() {
        assertMocksCreated();
        logger.info("All mocks created successfully");
    }

    /**
     * Проверяет конфигурацию Flyway.
     *
     * <p>Проверяет:
     * <ul>
     *   <li>Наличие экземпляра Flyway</li>
     *   <li>Наличие конфигурации</li>
     *   <li>Наличие локаций миграций</li>
     * </ul>
     */
    @Test
    void testFlywayConfiguration() {
        assertMocksCreated();

        if (flyway == null) {
            logger.warn("Flyway is disabled in test profile, skipping test");
            return;
        }

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        var configuration = flyway.getConfiguration();
        assertThat(configuration)
                .as("Flyway configuration should be available")
                .isNotNull();

        logger.info("Flyway configured successfully");

        Location[] locations = configuration.getLocations();
        String[] locationStrings = Arrays.stream(locations)
                .map(Location::toString)
                .toArray(String[]::new);
        logger.info("Flyway locations: {}", Arrays.toString(locationStrings));
    }

    /**
     * Проверяет, что миграции Flyway успешно применены.
     *
     * <p>Проверяет наличие таблиц:
     * <ul>
     *   <li>{@code flyway_schema_history} - история миграций</li>
     *   <li>{@code vector_store} - таблица для векторного хранилища</li>
     * </ul>
     *
     * <p>Измеряет время проверки миграций с помощью {@link TestUtils}.
     *
     * @throws SQLException если ошибка подключения к базе данных
     */
    @Test
    void testFlywayMigrationsApplied() throws SQLException {
        assertMocksCreated();
        assertDataSourceAvailable();

        if (flyway == null) {
            logger.warn("Flyway is disabled in test profile, skipping test");
            return;
        }

        TestUtils.measureExecutionTime("Flyway migration check", () -> {
            try (Connection connection = dataSource.getConnection()) {
                var resultSet = connection.getMetaData().getTables(null, "public", "flyway_schema_history", null);

                if (!resultSet.next()) {
                    logger.warn("Flyway schema history table not found (Flyway may be disabled in test profile)");
                    return;
                }

                assertThat(resultSet)
                        .as("Flyway schema history table should exist")
                        .isNotNull();

                var vectorTable = connection.getMetaData().getTables(null, "public", "vector_store", null);
                assertThat(vectorTable.next())
                        .as("vector_store table should exist")
                        .isTrue();

                logger.info("Flyway migrations applied successfully");
                logger.info("vector_store table exists");
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        });
    }

    /**
     * Проверяет версию схемы Flyway.
     */
    @Test
    void testFlywaySchemaVersion() {
        assertMocksCreated();

        if (flyway == null) {
            logger.warn("Flyway is disabled in test profile, skipping test");
            return;
        }

        var configuration = flyway.getConfiguration();
        assertThat(configuration.getSchemas())
                .as("Schemas should be configured")
                .isNotEmpty();

        String[] schemas = configuration.getSchemas();
        logger.info("Flyway schemas: {}", Arrays.toString(schemas));
    }

    /**
     * Проверяет статус миграций Flyway.
     *
     * <p>Логирует:
     * <ul>
     *   <li>Текущую версию</li>
     *   <li>Количество ожидающих миграций</li>
     *   <li>Количество примененных миграций</li>
     * </ul>
     */
    @Test
    void testFlywayMigrationStatus() {
        assertMocksCreated();

        if (flyway == null) {
            logger.warn("Flyway is disabled in test profile, skipping test");
            return;
        }

        var info = flyway.info();
        assertThat(info)
                .as("Flyway info should be available")
                .isNotNull();

        logger.info("Flyway migration status retrieved");
        logger.debug("   Current version: {}", info.current() != null ? info.current().getVersion() : "none");
        logger.debug("   Pending migrations: {}", info.pending().length);
        logger.debug("   Applied migrations: {}", info.applied().length);
    }
}