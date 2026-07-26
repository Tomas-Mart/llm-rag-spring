package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import com.example.rag.support.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

/**
 * Тест для проверки конфигурации AI компонентов.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет создание и настройку клиентов Ollama в тестовом профиле.</p>
 *
 * <h2>Тестируемые компоненты</h2>
 * <ul>
 *   <li>{@code OllamaApi} - клиент для Ollama API</li>
 *   <li>{@code OllamaChatModel} - модель чата</li>
 *   <li>{@code ChatClient} - высокоуровневый клиент</li>
 *   <li>{@code VectorStore} - векторное хранилище</li>
 *   <li>{@code OllamaChatOptions} - опции конфигурации</li>
 * </ul>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Все компоненты заменены на моки</li>
 *   <li>Используется реальный PostgreSQL из application-test.yml</li>
 *   <li>Проверяются свойства конфигурации</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see BaseTest
 * @since 1.0
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Epic("Модульные тесты")
@Feature("Конфигурация AI")
class AiConfigTest extends BaseTest {

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private OllamaChatOptions ollamaOptions;

    // ============================================================
    // ТЕСТЫ КОМПОНЕНТОВ (МОКИ)
    // ============================================================

    @Test
    @Description("Проверка создания мока OllamaApi")
    @Story("AI компоненты")
    @Severity(SeverityLevel.NORMAL)
    void testOllamaApiBean() {
        assertThat(ollamaApi).isNotNull();
        assertThat(mockingDetails(ollamaApi).isMock()).isTrue();
        log.info("✅ OllamaApi mock created");
    }

    @Test
    @Description("Проверка создания мока OllamaChatModel")
    @Story("AI компоненты")
    @Severity(SeverityLevel.NORMAL)
    void testChatModelBean() {
        assertThat(ollamaChatModel).isNotNull();
        assertThat(mockingDetails(ollamaChatModel).isMock()).isTrue();
        log.info("✅ OllamaChatModel mock created");
    }

    @Test
    @Description("Проверка создания мока ChatClient")
    @Story("AI компоненты")
    @Severity(SeverityLevel.NORMAL)
    void testChatClientBean() {
        assertThat(chatClient).isNotNull();
        assertThat(mockingDetails(chatClient).isMock()).isTrue();
        log.info("✅ ChatClient mock created");
    }

    @Test
    @Description("Проверка создания мока VectorStore")
    @Story("AI компоненты")
    @Severity(SeverityLevel.NORMAL)
    void testVectorStoreBean() {
        assertThat(vectorStore).isNotNull();
        assertThat(mockingDetails(vectorStore).isMock()).isTrue();
        log.info("✅ VectorStore mock created");
    }

    // ============================================================
    // ТЕСТЫ КОНФИГУРАЦИИ
    // ============================================================

    @Test
    @Description("Проверка URL Ollama API")
    @Story("Конфигурация")
    @Severity(SeverityLevel.NORMAL)
    void testOllamaApiUrl() {
        String ollamaUrl = environment.getProperty(
                "spring.ai.ollama.base-url",
                "http://localhost:11434"
        );

        assertThat(ollamaUrl)
                .as("Ollama URL should be configured")
                .isEqualTo("http://localhost:11434");

        log.info("✅ Ollama URL: {}", ollamaUrl);
    }

    @Test
    @Description("Проверка опций OllamaChatOptions")
    @Story("Конфигурация")
    @Severity(SeverityLevel.NORMAL)
    void testOllamaOptionsConfiguration() {
        if (ollamaOptions == null) {
            verifyOptionsFromEnvironment();
            return;
        }

        verifyOptionsFromBean();
    }

    @Test
    @Description("Проверка модели эмбеддингов")
    @Story("Конфигурация")
    @Severity(SeverityLevel.NORMAL)
    void testEmbeddingModelConfiguration() {
        String embeddingModel = environment.getProperty(
                "spring.ai.ollama.embedding.options.model",
                "nomic-embed-text:v1.5"
        );

        assertThat(embeddingModel)
                .as("Embedding model should be configured")
                .isEqualTo("nomic-embed-text:v1.5");

        log.info("✅ Embedding model: {}", embeddingModel);
    }

    @Test
    @Description("Проверка конфигурации VectorStore")
    @Story("Конфигурация")
    @Severity(SeverityLevel.NORMAL)
    void testVectorStoreConfiguration() {
        Integer dimensions = environment.getProperty(
                "spring.ai.vectorstore.pgvector.dimensions", Integer.class);
        assertThat(dimensions)
                .as("Vector dimensions should be configured")
                .isEqualTo(768);

        String indexType = environment.getProperty(
                "spring.ai.vectorstore.pgvector.index-type");
        assertThat(indexType)
                .as("Index type should be configured")
                .isEqualTo("HNSW");

        String distanceType = environment.getProperty(
                "spring.ai.vectorstore.pgvector.distance-type");
        assertThat(distanceType)
                .as("Distance type should be configured")
                .isEqualTo("EUCLIDEAN_DISTANCE");

        log.info("✅ VectorStore: dims={}, index={}, distance={}",
                dimensions, indexType, distanceType);
    }

    @Test
    @Description("Проверка DataSource (без H2)")
    @Story("Конфигурация")
    @Severity(SeverityLevel.CRITICAL)
    void testDataSourceConfiguration() {
        String url = environment.getProperty("spring.datasource.url");
        assertThat(url)
                .as("DataSource should be PostgreSQL")
                .contains("postgresql")
                .doesNotContain("h2");

        String driver = environment.getProperty("spring.datasource.driver-class-name");
        assertThat(driver)
                .as("Driver should be PostgreSQL")
                .isEqualTo("org.postgresql.Driver");

        log.info("✅ DataSource: PostgreSQL");
        log.debug("   URL: {}", url);
    }

    @Test
    @Description("Проверка всех свойств окружения")
    @Story("Конфигурация")
    @Severity(SeverityLevel.CRITICAL)
    void testEnvironmentProperties() {
        assertThat(environment.getProperty("spring.ai.ollama.base-url"))
                .isEqualTo("http://localhost:11434");

        assertThat(environment.getProperty("spring.ai.ollama.chat.options.model"))
                .isEqualTo("qwen2.5-coder:7b");

        assertThat(environment.getProperty("spring.ai.ollama.chat.options.temperature", Double.class))
                .isEqualTo(0.2);

        assertThat(environment.getProperty("spring.ai.vectorstore.pgvector.dimensions", Integer.class))
                .isEqualTo(768);

        assertThat(environment.getProperty("spring.ai.ollama.embedding.options.model"))
                .isEqualTo("nomic-embed-text:v1.5");

        assertThat(environment.getProperty("spring.datasource.url"))
                .doesNotContain("h2");

        log.info("✅ All environment properties verified (no H2)");
    }

    @Test
    @Description("Комплексная проверка AI конфигурации")
    @Story("Интеграция")
    @Severity(SeverityLevel.CRITICAL)
    void testAIConfigurationIntegration() {
        // Проверяем моки
        assertThat(ollamaApi).isNotNull();
        assertThat(ollamaChatModel).isNotNull();
        assertThat(vectorStore).isNotNull();
        assertThat(chatClient).isNotNull();

        assertThat(mockingDetails(ollamaApi).isMock()).isTrue();
        assertThat(mockingDetails(ollamaChatModel).isMock()).isTrue();
        assertThat(mockingDetails(vectorStore).isMock()).isTrue();
        assertThat(mockingDetails(chatClient).isMock()).isTrue();

        // Проверяем DataSource
        String url = environment.getProperty("spring.datasource.url");
        assertThat(url).contains("postgresql");
        assertThat(url).doesNotContain("h2");

        log.info("✅ AI components configured as mocks");
        log.info("✅ Using real PostgreSQL (no H2)");
        log.debug("   Bean count: {}", applicationContext.getBeanDefinitionCount());
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Проверяет опции через Environment.
     */
    private void verifyOptionsFromEnvironment() {
        String model = environment.getProperty("spring.ai.ollama.chat.options.model");
        assertThat(model)
                .as("Model should be configured")
                .isEqualTo("qwen2.5-coder:7b");

        Double temperature = environment.getProperty(
                "spring.ai.ollama.chat.options.temperature", Double.class);
        assertThat(temperature)
                .as("Temperature should be configured")
                .isEqualTo(0.2);

        Integer numCtx = environment.getProperty(
                "spring.ai.ollama.chat.options.num-ctx", Integer.class);
        assertThat(numCtx)
                .as("Context size should be configured")
                .isEqualTo(4096);

        log.info("✅ Options from environment: model={}, temp={}, ctx={}",
                model, temperature, numCtx);
    }

    /**
     * Проверяет опции через OllamaChatOptions бин.
     */
    private void verifyOptionsFromBean() {
        assertThat(ollamaOptions)
                .as("OllamaOptions should be created")
                .isNotNull();

        assertThat(ollamaOptions.getModel())
                .as("Model should be configured")
                .isEqualTo("qwen2.5-coder:7b");

        assertThat(ollamaOptions.getTemperature())
                .as("Temperature should be configured")
                .isEqualTo(0.2);

        assertThat(ollamaOptions.getNumCtx())
                .as("Context size should be configured")
                .isEqualTo(4096);

        log.info("✅ Options from bean: model={}, temp={}, ctx={}",
                ollamaOptions.getModel(),
                ollamaOptions.getTemperature(),
                ollamaOptions.getNumCtx());
    }
}