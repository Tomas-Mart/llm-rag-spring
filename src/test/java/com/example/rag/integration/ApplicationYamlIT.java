package com.example.rag.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import com.example.rag.support.BaseIntegrationTest;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки загрузки конфигурационных свойств из application.yml.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет все основные настройки приложения в интеграционном профиле.</p>
 *
 * <h2>Тестируемые конфигурации</h2>
 * <ul>
 *   <li>Имя приложения</li>
 *   <li>Настройки базы данных</li>
 *   <li>Настройки JPA и Flyway</li>
 *   <li>Настройки Ollama</li>
 *   <li>Настройки Vector Store</li>
 *   <li>Настройки логирования</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see BaseIntegrationTest
 * @see Environment
 * @since 1.0
 */
@Slf4j
@Epic("Интеграционные тесты")
@Feature("Конфигурация приложения")
class ApplicationYamlIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String EXPECTED_APP_NAME = "llm-rag-spring-integration-test";
    private static final String EXPECTED_OLLAMA_URL = "http://localhost:11434";
    private static final String EXPECTED_MODEL = "qwen2.5-coder:7b";
    private static final String EXPECTED_TEMPERATURE = "0.2";
    private static final String EXPECTED_NUM_CTX = "4096";
    private static final String EXPECTED_EMBEDDING_MODEL = "nomic-embed-text:v1.5";
    private static final int EXPECTED_VECTOR_DIMENSIONS = 768;

    private static final String[] REQUIRED_PROPERTIES = {
            "spring.application.name",
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.ai.ollama.base-url",
            "spring.ai.ollama.chat.options.model"
    };

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private Environment environment;

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка имени приложения")
    @Story("Конфигурация приложения")
    @Severity(SeverityLevel.NORMAL)
    void testApplicationName() {
        logTestStart("Testing application name");

        String appName = environment.getProperty("spring.application.name");
        assertThat(appName)
                .as("Application name should be configured")
                .isEqualTo(EXPECTED_APP_NAME);

        log.info("✅ Application name: {}", appName);
        logTestSuccess("Application name verified");
    }

    @Test
    @Description("Проверка URL базы данных")
    @Story("Конфигурация БД")
    @Severity(SeverityLevel.CRITICAL)
    void testDatabaseUrl() {
        logTestStart("Testing database URL");

        String url = environment.getProperty("spring.datasource.url");
        assertThat(url)
                .as("Database URL should be configured")
                .isNotNull()
                .contains("postgresql")
                .doesNotContain("h2");

        log.info("✅ Database URL: {}", url);
        logTestSuccess("Database URL verified");
    }

    @Test
    @Description("Проверка конфигурации DataSource")
    @Story("Конфигурация БД")
    @Severity(SeverityLevel.CRITICAL)
    void testDatasourceConfiguration() {
        logTestStart("Testing datasource configuration");

        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        String driver = environment.getProperty("spring.datasource.driver-class-name");

        assertThat(url).isNotNull().isNotEmpty();
        assertThat(username).isNotNull();
        assertThat(password).isNotNull();
        assertThat(driver)
                .as("Driver should be PostgreSQL")
                .isEqualTo("org.postgresql.Driver");

        log.info("✅ Datasource: URL={}, Username={}, Driver={}", url, username, driver);
        logTestSuccess("Datasource configuration verified");
    }

    @Test
    @Description("Проверка конфигурации JPA")
    @Story("Конфигурация JPA")
    @Severity(SeverityLevel.NORMAL)
    void testJpaConfiguration() {
        logTestStart("Testing JPA configuration");

        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        String showSql = environment.getProperty("spring.jpa.show-sql");

        assertThat(ddlAuto)
                .as("DDL auto should be 'validate'")
                .isEqualTo("validate");

        log.info("✅ JPA: ddl-auto={}, show-sql={}", ddlAuto, showSql);
        logTestSuccess("JPA configuration verified");
    }

    @Test
    @Description("Проверка конфигурации Flyway")
    @Story("Конфигурация Flyway")
    @Severity(SeverityLevel.NORMAL)
    void testFlywayConfiguration() {
        logTestStart("Testing Flyway configuration");

        String enabled = environment.getProperty("spring.flyway.enabled");
        String locations = environment.getProperty("spring.flyway.locations");

        assertThat(enabled)
                .as("Flyway should be enabled")
                .isEqualTo("true");

        assertThat(locations)
                .as("Flyway locations should be configured")
                .isEqualTo("classpath:db/migration");

        log.info("✅ Flyway: enabled={}, locations={}", enabled, locations);
        logTestSuccess("Flyway configuration verified");
    }

    @Test
    @Description("Проверка конфигурации Ollama")
    @Story("Конфигурация AI")
    @Severity(SeverityLevel.CRITICAL)
    void testOllamaConfiguration() {
        logTestStart("Testing Ollama configuration");

        String baseUrl = environment.getProperty("spring.ai.ollama.base-url");
        String model = environment.getProperty("spring.ai.ollama.chat.options.model");
        String temperature = environment.getProperty("spring.ai.ollama.chat.options.temperature");
        String numCtx = environment.getProperty("spring.ai.ollama.chat.options.num-ctx");
        String embeddingModel = environment.getProperty("spring.ai.ollama.embedding.options.model");

        assertThat(baseUrl).isEqualTo(EXPECTED_OLLAMA_URL);
        assertThat(model).isEqualTo(EXPECTED_MODEL);
        assertThat(temperature).isEqualTo(EXPECTED_TEMPERATURE);
        assertThat(numCtx).isEqualTo(EXPECTED_NUM_CTX);
        assertThat(embeddingModel).isEqualTo(EXPECTED_EMBEDDING_MODEL);

        log.info("✅ Ollama: URL={}, Model={}, Temp={}, Ctx={}, Embed={}",
                baseUrl, model, temperature, numCtx, embeddingModel);
        logTestSuccess("Ollama configuration verified");
    }

    @Test
    @Description("Проверка конфигурации Vector Store")
    @Story("Конфигурация Vector Store")
    @Severity(SeverityLevel.NORMAL)
    void testVectorstoreConfiguration() {
        logTestStart("Testing Vector Store configuration");

        String distanceType = environment.getProperty("spring.ai.vectorstore.pgvector.distance-type");
        String indexType = environment.getProperty("spring.ai.vectorstore.pgvector.index-type");
        String dimensions = environment.getProperty("spring.ai.vectorstore.pgvector.dimensions");
        String tableName = environment.getProperty("spring.ai.vectorstore.pgvector.table-name");

        assertThat(distanceType)
                .as("Distance type should be configured")
                .isIn("EUCLIDEAN_DISTANCE", "COSINE_DISTANCE", "DOT_PRODUCT");

        assertThat(indexType)
                .as("Index type should be configured")
                .isIn("HNSW", "IVFFLAT");

        assertThat(dimensions)
                .as("Dimensions should be configured")
                .isEqualTo(String.valueOf(EXPECTED_VECTOR_DIMENSIONS));

        assertThat(tableName)
                .as("Table name should be configured")
                .isEqualTo("vector_store");

        log.info("✅ Vector Store: distance={}, index={}, dims={}, table={}",
                distanceType, indexType, dimensions, tableName);
        logTestSuccess("Vector Store configuration verified");
    }

    @Test
    @Description("Проверка конфигурации логирования")
    @Story("Конфигурация логирования")
    @Severity(SeverityLevel.MINOR)
    void testLoggingConfiguration() {
        logTestStart("Testing logging configuration");

        String loggingLevel = environment.getProperty("logging.level.org.springframework.ai");
        assertThat(loggingLevel)
                .as("Logging level should be configured")
                .isIn("DEBUG", "INFO", "WARN", "ERROR");

        log.info("✅ Logging level: {}", loggingLevel);
        logTestSuccess("Logging configuration verified");
    }

    @Test
    @Description("Проверка наличия всех обязательных свойств")
    @Story("Конфигурация приложения")
    @Severity(SeverityLevel.CRITICAL)
    void testAllRequiredPropertiesArePresent() {
        logTestStart("Testing required properties");

        for (String property : REQUIRED_PROPERTIES) {
            assertThat(environment.getProperty(property))
                    .withFailMessage("Property " + property + " is missing")
                    .isNotNull();
        }

        log.info("✅ All required properties are present");
        logTestSuccess("Required properties verified");
    }

    @Test
    @Description("Проверка формата URL Ollama")
    @Story("Конфигурация AI")
    @Severity(SeverityLevel.NORMAL)
    void testOllamaUrlFormat() {
        logTestStart("Testing Ollama URL format");

        String url = environment.getProperty("spring.ai.ollama.base-url");

        assertThat(url)
                .as("Ollama URL should be in correct format")
                .startsWith("http://")
                .contains("localhost")
                .contains("11434");

        log.info("✅ Ollama URL format is correct");
        logTestSuccess("Ollama URL format verified");
    }

    @Test
    @Description("Проверка отсутствия H2 в конфигурации")
    @Story("Конфигурация БД")
    @Severity(SeverityLevel.CRITICAL)
    void testNoH2InUrl() {
        logTestStart("Testing no H2 in configuration");

        String url = environment.getProperty("spring.datasource.url");
        assertThat(url)
                .as("Database URL should not contain H2")
                .doesNotContain("h2");

        String driver = environment.getProperty("spring.datasource.driver-class-name");
        assertThat(driver)
                .as("Driver should be PostgreSQL, not H2")
                .isNotEqualTo("org.h2.Driver");

        log.info("✅ No H2 in configuration");
        logTestSuccess("H2 absence verified");
    }
}