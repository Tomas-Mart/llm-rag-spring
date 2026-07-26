package com.example.rag.integration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.rag.support.BaseIntegrationTest;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Интеграционный тест для проверки миграций Flyway.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет корректность работы Flyway миграций в реальной PostgreSQL.
 * Все тесты выполняются в изолированной транзакции, которая автоматически откатывается.</p>
 *
 * <h2>Тестируемые аспекты</h2>
 * <ul>
 *   <li>Конфигурация Flyway (locations, schemas, параметры)</li>
 *   <li>Применение миграций (проверка таблиц)</li>
 *   <li>Статус миграций (текущая версия, количество)</li>
 *   <li>Валидация миграций (проверка целостности)</li>
 *   <li>Ремонт миграций (исправление проблем)</li>
 * </ul>
 *
 * <h2>Важно</h2>
 * <ul>
 *   <li>Все аннотации наследуются от {@link BaseIntegrationTest}</li>
 *   <li>Для логирования используется Lombok {@code log} из родительского класса</li>
 *   <li>⚠️ Тест {@code testFlywayCleanAndMigrate} удален - он очищал базу данных</li>
 * </ul>
 *
 * <h2>Пример запуска</h2>
 * <pre>{@code
 * // Запустить все тесты
 * mvn test -Dtest=FlywayIT
 *
 * // Запустить конкретный тест
 * mvn test -Dtest=FlywayIT#testFlywayMigrationsApplied
 * }</pre>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see BaseIntegrationTest
 * @see Flyway
 * @since 1.0
 */
@Slf4j
@Epic("Интеграционные тесты")
@Feature("Миграции БД")
class FlywayIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    /**
     * Экземпляр Flyway для проверки миграций.
     * Автоматически внедряется Spring в интеграционном профиле.
     */
    @Autowired
    private Flyway flyway;

    // ============================================================
    // ТЕСТЫ КОНФИГУРАЦИИ
    // ============================================================

    /**
     * Проверяет конфигурацию Flyway.
     */
    @Test
    void testFlywayConfiguration() {
        logTestStart("Testing Flyway configuration");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        var configuration = flyway.getConfiguration();
        assertThat(configuration)
                .as("Flyway configuration should be available")
                .isNotNull();

        logConfigurationDetails(configuration);

        logTestSuccess("Flyway configuration verified");
    }

    // ============================================================
    // ТЕСТЫ МИГРАЦИЙ
    // ============================================================

    /**
     * Проверяет, что миграции Flyway успешно применены.
     *
     * @throws SQLException если ошибка подключения к БД
     */
    @Test
    void testFlywayMigrationsApplied() throws SQLException {
        logTestStart("Testing Flyway migrations applied");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        var info = flyway.info();

        assertThat(info.applied())
                .as("Flyway migrations should be applied")
                .isNotEmpty();

        log.info("✅ Flyway migrations applied successfully");
        log.debug("   📊 Applied: {}", info.applied().length);
        log.debug("   📊 Pending: {}", info.pending().length);

        verifyTablesExist();

        logTestSuccess("Flyway migrations applied successfully");
    }

    // ============================================================
    // ТЕСТЫ СТАТУСА
    // ============================================================

    /**
     * Проверяет статус миграций Flyway.
     */
    @Test
    void testFlywayMigrationStatus() {
        logTestStart("Testing Flyway migration status");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        var info = flyway.info();
        assertThat(info)
                .as("Flyway info should be available")
                .isNotNull();

        var current = info.current();
        var pending = info.pending();
        var applied = info.applied();

        logMigrationStatus(current, pending, applied);
        logAppliedMigrations(applied);

        logTestSuccess("Flyway migration status verified");
    }

    // ============================================================
    // ТЕСТЫ ВЕРСИЙ
    // ============================================================

    /**
     * Проверяет версию схемы Flyway.
     */
    @Test
    void testFlywaySchemaVersion() {
        logTestStart("Testing Flyway schema version");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        var info = flyway.info();
        var current = info.current();

        if (current != null) {
            log.info("✅ Current schema version: {}", current.getVersion());
            log.info("   📝 Description: {}", current.getDescription());
            log.info("   📊 State: {}", current.getState());
        } else {
            log.warn("⚠️ No schema version found (database not initialized)");
        }

        logTestSuccess("Flyway schema version checked");
    }

    // ============================================================
    // ТЕСТЫ ВАЛИДАЦИИ
    // ============================================================

    /**
     * Проверяет валидацию миграций Flyway.
     *
     * <p>Метод validate() проверяет соответствие примененных миграций
     * текущим файлам миграций. В случае несоответствия выбрасывается
     * исключение {@link org.flywaydb.core.api.FlywayException}.</p>
     */
    @Test
    void testFlywayValidation() {
        logTestStart("Testing Flyway validation");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        assertThatCode(() -> flyway.validate())
                .as("Flyway validation should pass without exceptions")
                .doesNotThrowAnyException();

        log.info("✅ Flyway validation passed successfully");

        logTestSuccess("Flyway validation completed");
    }

    // ============================================================
    // ТЕСТЫ REPAIR
    // ============================================================

    /**
     * Проверяет ремонт миграций Flyway.
     *
     * <p>Метод repair() исправляет проблемы в таблице истории миграций:
     * <ul>
     *   <li>Несоответствие контрольных сумм</li>
     *   <li>Отсутствующие миграции</li>
     *   <li>Поврежденные записи</li>
     * </ul>
     */
    @Test
    void testFlywayRepair() {
        logTestStart("Testing Flyway repair");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        assertThatCode(() -> flyway.repair())
                .as("Flyway repair should complete without exceptions")
                .doesNotThrowAnyException();

        log.info("✅ Flyway repair completed successfully");

        var info = flyway.info();
        assertThat(info)
                .as("Flyway info should be available after repair")
                .isNotNull();

        logTestSuccess("Flyway repair completed");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Логирует детали конфигурации Flyway.
     *
     * @param configuration конфигурация Flyway
     */
    private void logConfigurationDetails(org.flywaydb.core.api.configuration.Configuration configuration) {
        Location[] locations = configuration.getLocations();
        String[] locationStrings = Arrays.stream(locations)
                .map(Location::toString)
                .toArray(String[]::new);

        log.info("✅ Flyway configured successfully");
        log.info("   📁 Locations: {}", Arrays.toString(locationStrings));
        log.info("   📊 Schemas: {}", Arrays.toString(configuration.getSchemas()));
        log.info("   🔄 Baseline on migrate: {}", configuration.isBaselineOnMigrate());
        log.info("   ✅ Validate on migrate: {}", configuration.isValidateOnMigrate());
    }

    /**
     * Проверяет существование всех необходимых таблиц.
     *
     * @throws SQLException если ошибка подключения к БД
     */
    private void verifyTablesExist() throws SQLException {
        assertDataSourceAvailable();

        try (Connection connection = dataSource.getConnection()) {
            String[] tablesToCheck = {"flyway_schema_history", "vector_store", "documents"};

            for (String tableName : tablesToCheck) {
                var table = connection.getMetaData()
                        .getTables(null, "public", tableName, null);
                assertThat(table.next())
                        .as("%s table should exist", tableName)
                        .isTrue();
                log.info("   ✅ {} exists", tableName);
            }

            log.info("✅ All required tables exist");
        }
    }

    /**
     * Логирует статус миграций.
     *
     * @param current текущая миграция
     * @param pending ожидающие миграции
     * @param applied примененные миграции
     */
    private void logMigrationStatus(
            org.flywaydb.core.api.MigrationInfo current,
            org.flywaydb.core.api.MigrationInfo[] pending,
            org.flywaydb.core.api.MigrationInfo[] applied
    ) {

        String currentVersion = current != null
                ? current.getVersion().toString()
                : "none";

        log.info("✅ Flyway migration status:");
        log.info("   📌 Current version: {}", currentVersion);
        log.info("   ✅ Applied: {}", applied.length);
        log.info("   ⏳ Pending: {}", pending.length);
    }

    /**
     * Логирует список примененных миграций.
     *
     * @param applied примененные миграции
     */
    private void logAppliedMigrations(org.flywaydb.core.api.MigrationInfo[] applied) {
        if (applied.length > 0) {
            log.debug("   📋 Applied migrations:");
            for (var migration : applied) {
                log.debug("      - v{}: {} (success: {})",
                        migration.getVersion(),
                        migration.getDescription(),
                        migration.getState());
            }
        }
    }
}