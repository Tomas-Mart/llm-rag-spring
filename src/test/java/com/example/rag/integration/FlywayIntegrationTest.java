package com.example.rag.integration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.example.rag.support.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Интеграционный тест для проверки миграций Flyway.
 * Использует реальную PostgreSQL с включенным Flyway.
 *
 * <p>Тестируемые аспекты:
 * <ul>
 *   <li>Конфигурация Flyway</li>
 *   <li>Применение миграций</li>
 *   <li>Схема базы данных</li>
 *   <li>Статус миграций</li>
 *   <li>Валидация миграций</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 2.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("integration-test")
@Tag("integration")
@Transactional
class FlywayIntegrationTest extends BaseIntegrationTest {

    /**
     * Экземпляр Flyway для проверки миграций.
     * В интеграционном профиле Flyway включен.
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

        logger.info("✅ Flyway configured successfully");

        Location[] locations = configuration.getLocations();
        String[] locationStrings = Arrays.stream(locations)
                .map(Location::toString)
                .toArray(String[]::new);

        logger.info("   📁 Flyway locations: {}", Arrays.toString(locationStrings));
        logger.info("   📊 Flyway schemas: {}", Arrays.toString(configuration.getSchemas()));
        logger.info("   🔄 Baseline on migrate: {}", configuration.isBaselineOnMigrate());
        logger.info("   ✅ Validate on migrate: {}", configuration.isValidateOnMigrate());

        logTestSuccess("Flyway configuration verified");
    }

    // ============================================================
    // ТЕСТЫ МИГРАЦИЙ
    // ============================================================

    /**
     * Проверяет, что миграции Flyway успешно применены.
     */
    @Test
    void testFlywayMigrationsApplied() throws SQLException {
        logTestStart("Testing Flyway migrations applied");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        // 1. Проверяем через Flyway info
        var info = flyway.info();
        assertThat(info.applied())
                .as("Flyway migrations should be applied")
                .isNotEmpty();

        logger.info("✅ Flyway migrations applied successfully");
        logger.debug("   📊 Applied migrations: {}", info.applied().length);
        logger.debug("   📊 Pending migrations: {}", info.pending().length);

        // 2. Проверяем наличие таблиц через DataSource
        assertDataSourceAvailable();

        try (Connection connection = dataSource.getConnection()) {
            // Проверяем таблицу flyway_schema_history
            var historyTable = connection.getMetaData()
                    .getTables(null, "public", "flyway_schema_history", null);
            assertThat(historyTable.next())
                    .as("flyway_schema_history table should exist")
                    .isTrue();
            logger.info("   ✅ flyway_schema_history table exists");

            // Проверяем таблицу vector_store
            var vectorTable = connection.getMetaData()
                    .getTables(null, "public", "vector_store", null);
            assertThat(vectorTable.next())
                    .as("vector_store table should exist")
                    .isTrue();
            logger.info("   ✅ vector_store table exists");

            // Проверяем таблицу documents
            var documentsTable = connection.getMetaData()
                    .getTables(null, "public", "documents", null);
            assertThat(documentsTable.next())
                    .as("documents table should exist")
                    .isTrue();
            logger.info("   ✅ documents table exists");

            logger.info("✅ All required tables exist");
        }

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

        logger.info("✅ Flyway migration status:");
        logger.info("   📌 Current version: {}", current != null ? current.getVersion() : "none");
        logger.info("   ✅ Applied migrations: {}", applied.length);
        logger.info("   ⏳ Pending migrations: {}", pending.length);

        // Логируем все примененные миграции
        if (applied.length > 0) {
            logger.debug("   📋 Applied migrations:");
            for (var migration : applied) {
                logger.debug("      - v{}: {} (success: {})",
                        migration.getVersion(),
                        migration.getDescription(),
                        migration.getState());
            }
        }

        logTestSuccess("Flyway migration status verified");
    }

    // ============================================================
    // ТЕСТЫ ВОССТАНОВЛЕНИЯ
    // ============================================================

    /**
     * Проверяет, что миграции можно откатить и применить заново.
     *
     * <p>⚠️ ВНИМАНИЕ: Этот тест очищает базу данных!
     * Используйте только в изолированной тестовой среде.
     */
    @Test
    void testFlywayCleanAndMigrate() throws SQLException {
        logTestStart("Testing Flyway clean and migrate");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        // Получаем информацию до
        var infoBefore = flyway.info();
        int beforeCount = infoBefore.applied().length;
        logger.debug("   Migrations before clean: {}", beforeCount);

        // Очищаем и заново мигрируем
        flyway.clean();
        logger.debug("   🧹 Database cleaned");

        flyway.migrate();
        logger.debug("   📥 Migrations reapplied");

        // Получаем информацию после
        var infoAfter = flyway.info();
        int afterCount = infoAfter.applied().length;

        assertThat(afterCount)
                .as("Migrations should be applied after clean and migrate")
                .isEqualTo(beforeCount);

        logger.info("✅ Flyway clean and migrate successful");
        logger.info("   📊 Migrations before: {}", beforeCount);
        logger.info("   📊 Migrations after: {}", afterCount);

        logTestSuccess("Flyway clean and migrate completed");
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
            logger.info("✅ Current schema version: {}", current.getVersion());
            logger.info("   📝 Description: {}", current.getDescription());
            logger.info("   📊 State: {}", current.getState());
        } else {
            logger.warn("⚠️ No schema version found (database not initialized)");
        }

        logTestSuccess("Flyway schema version checked");
    }

    // ============================================================
    // ТЕСТЫ ВАЛИДАЦИИ
    // ============================================================

    /**
     * Проверяет валидацию миграций Flyway.
     *
     * <p>Метод validate() проверяет, что все примененные миграции
     * соответствуют текущим файлам миграций. Если есть несоответствия,
     * будет выброшено исключение FlywayValidateException.
     *
     * <p>Так как validate() возвращает void, мы проверяем,
     * что метод выполняется без исключений.
     */
    @Test
    void testFlywayValidation() {
        logTestStart("Testing Flyway validation");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        // Проверяем, что валидация проходит без ошибок
        // validate() возвращает void, но выбрасывает исключение при ошибке
        assertThatCode(() -> flyway.validate())
                .as("Flyway validation should pass without exceptions")
                .doesNotThrowAnyException();

        logger.info("✅ Flyway validation passed successfully");
        logger.debug("   Flyway validation completed without errors");

        logTestSuccess("Flyway validation completed");
    }

    // ============================================================
    // ТЕСТЫ REPAIR
    // ============================================================

    /**
     * Проверяет ремонт миграций Flyway.
     *
     * <p>Метод repair() исправляет проблемы в таблице истории миграций.
     */
    @Test
    void testFlywayRepair() {
        logTestStart("Testing Flyway repair");

        assertThat(flyway)
                .as("Flyway should be configured")
                .isNotNull();

        // Проверяем, что repair выполняется без ошибок
        assertThatCode(() -> flyway.repair())
                .as("Flyway repair should complete without exceptions")
                .doesNotThrowAnyException();

        logger.info("✅ Flyway repair completed successfully");

        // Проверяем, что после repair информация доступна
        var info = flyway.info();
        assertThat(info)
                .as("Flyway info should be available after repair")
                .isNotNull();

        logTestSuccess("Flyway repair completed");
    }
}