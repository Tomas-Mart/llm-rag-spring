package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import com.example.rag.support.BaseTest;
import com.example.rag.support.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки загрузки конфигурационных свойств из application.yml.
 * Проверяет все основные настройки приложения в тестовом профиле.
 *
 * <p>Тестируемые конфигурации:
 * <ul>
 *   <li>Имя приложения</li>
 *   <li>Настройки базы данных</li>
 *   <li>Настройки JPA</li>
 *   <li>Настройки Flyway</li>
 *   <li>Настройки Ollama</li>
 *   <li>Настройки Vector Store</li>
 *   <li>Настройки логирования</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
class ApplicationYamlTest extends BaseTest {

    /**
     * Окружение Spring для доступа к свойствам.
     */
    @Autowired
    private Environment environment;

    /**
     * Проверяет, что все моки созданы.
     */
    @Test
    void testMocksAreCreated() {
        assertMocksCreated();
        logger.info("All mocks created successfully");
    }

    /**
     * Проверяет имя приложения в тестовом профиле.
     * В тестовом профиле имя отличается от продакшена.
     */
    @Test
    void testApplicationName() {
        assertMocksCreated();

        String applicationName = environment.getProperty("spring.application.name");
        assertThat(applicationName)
                .as("Application name should be configured")
                .isEqualTo("llm-rag-spring-test");
        logger.info("Application name: {}", applicationName);
    }

    /**
     * Проверяет конфигурацию источника данных.
     *
     * <p>В тестовом профиле используется H2 база данных.
     */
    @Test
    void testDatasourceConfiguration() {
        assertMocksCreated();

        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        String driver = environment.getProperty("spring.datasource.driver-class-name");

        assertThat(url)
                .as("Database URL should use H2")
                .startsWith("jdbc:h2:");
        assertThat(username)
                .as("Database username should be sa")
                .isEqualTo("sa");
        assertThat(password)
                .as("Database password should be empty")
                .isEmpty();
        assertThat(driver)
                .as("Database driver should be H2")
                .isEqualTo("org.h2.Driver");

        logger.info("Datasource URL: {}", url);
        logger.info("Datasource Username: {}", username);
        logger.info("Datasource Driver: {}", driver);
    }

    /**
     * Проверяет конфигурацию JPA.
     *
     * <p>В тестовом профиле используется {@code create-drop} для DDL и
     * включено логирование SQL.
     */
    @Test
    void testJpaConfiguration() {
        assertMocksCreated();

        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        String showSql = environment.getProperty("spring.jpa.show-sql");

        assertThat(ddlAuto)
                .as("DDL auto should be create-drop for tests")
                .isEqualTo("create-drop");
        assertThat(showSql)
                .as("Show SQL should be true for tests")
                .isEqualTo("true");

        logger.info("JPA DDL auto: {}", ddlAuto);
        logger.info("JPA Show SQL: {}", showSql);
    }

    /**
     * Проверяет конфигурацию Flyway.
     *
     * <p>В тестовом профиле Flyway отключен.
     */
    @Test
    void testFlywayConfiguration() {
        assertMocksCreated();

        String enabled = environment.getProperty("spring.flyway.enabled");
        String locations = environment.getProperty("spring.flyway.locations");

        assertThat(enabled)
                .as("Flyway should be disabled in tests")
                .isEqualTo("false");
        assertThat(locations)
                .as("Flyway locations should not be set when disabled")
                .isNull();

        logger.info("Flyway enabled: {}", enabled);
        logger.info("Flyway locations: {}", locations);
    }

    /**
     * Проверяет конфигурацию Ollama.
     *
     * <p>Проверяет URL, модель, температуру, размер контекста и модель эмбеддингов.
     */
    @Test
    void testOllamaConfiguration() {
        assertMocksCreated();

        String baseUrl = environment.getProperty("spring.ai.ollama.base-url");
        String model = environment.getProperty("spring.ai.ollama.chat.options.model");
        String temperature = environment.getProperty("spring.ai.ollama.chat.options.temperature");
        String numCtx = environment.getProperty("spring.ai.ollama.chat.options.num-ctx");
        String embeddingModel = environment.getProperty("spring.ai.ollama.embedding.options.model");

        assertThat(baseUrl)
                .as("Ollama base URL should be configured")
                .isEqualTo("http://localhost:11434");
        assertThat(model)
                .as("Chat model should be configured")
                .isEqualTo("qwen2.5-coder:7b");
        assertThat(temperature)
                .as("Temperature should be configured")
                .isEqualTo("0.2");
        assertThat(numCtx)
                .as("Context size should be configured")
                .isEqualTo("4096");
        assertThat(embeddingModel)
                .as("Embedding model should be configured")
                .isEqualTo("nomic-embed-text:v1.5");

        logger.info("Ollama URL: {}", baseUrl);
        logger.info("Chat model: {}", model);
        logger.info("Temperature: {}", temperature);
        logger.info("Context size: {}", numCtx);
        logger.info("Embedding model: {}", embeddingModel);
    }

    /**
     * Проверяет конфигурацию Vector Store.
     */
    @Test
    void testVectorstoreConfiguration() {
        assertMocksCreated();

        String distanceType = environment.getProperty("spring.ai.vectorstore.pgvector.distance-type");

        assertThat(distanceType)
                .as("Distance type should be configured for tests")
                .isEqualTo("EUCLIDEAN_DISTANCE");

        logger.info("Distance type: {}", distanceType);
    }

    /**
     * Проверяет конфигурацию логирования.
     */
    @Test
    void testLoggingConfiguration() {
        assertMocksCreated();

        String loggingLevel = environment.getProperty("logging.level.org.springframework.ai");
        assertThat(loggingLevel)
                .as("Logging level should be configured")
                .isEqualTo("DEBUG");

        logger.info("Logging level: {}", loggingLevel);
    }

    /**
     * Проверяет, что все обязательные свойства присутствуют.
     */
    @Test
    void testAllRequiredPropertiesArePresent() {
        assertMocksCreated();

        String[] requiredProperties = {
                "spring.application.name",
                "spring.datasource.url",
                "spring.datasource.username",
                "spring.ai.ollama.base-url",
                "spring.ai.ollama.chat.options.model"
        };

        for (String property : requiredProperties) {
            assertThat(environment.getProperty(property))
                    .withFailMessage("Property " + property + " is missing")
                    .isNotNull();
        }

        if (TestUtils.isPropertySet("TEST_ENV")) {
            logger.info("Test environment variable is set: {}",
                    TestUtils.getPropertyOrDefault("TEST_ENV", "default"));
        }

        logger.info("All required properties are present");
    }

    /**
     * Проверяет формат URL базы данных.
     *
     * <p>URL должен соответствовать формату H2 с режимом PostgreSQL.
     */
    @Test
    void testDatabaseUrlFormat() {
        assertMocksCreated();

        String url = environment.getProperty("spring.datasource.url");

        assertThat(url)
                .as("Database URL should be in correct format")
                .startsWith("jdbc:h2:")
                .contains("mem")
                .contains("MODE=PostgreSQL");

        logger.info("Database URL format is correct");
    }

    /**
     * Проверяет формат URL Ollama.
     *
     * <p>URL должен использовать HTTP протокол, localhost и порт 11434.
     */
    @Test
    void testOllamaUrlFormat() {
        assertMocksCreated();

        String url = environment.getProperty("spring.ai.ollama.base-url");

        assertThat(url)
                .as("Ollama URL should be in correct format")
                .startsWith("http://")
                .contains("localhost")
                .contains("11434");

        logger.info("Ollama URL format is correct");
    }
}