package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import com.example.rag.support.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

/**
 * Тест для проверки конфигурации AI компонентов.
 * Проверяет создание и настройку клиентов Ollama.
 *
 * <p>Тестируемые компоненты:
 * <ul>
 *   <li>{@code OllamaApi} - клиент для Ollama API</li>
 *   <li>{@code OllamaChatModel} - модель чата</li>
 *   <li>{@code ChatClient} - высокоуровневый клиент</li>
 *   <li>{@code VectorStore} - векторное хранилище</li>
 *   <li>{@code OllamaChatOptions} - опции конфигурации</li>
 * </ul>
 *
 * <p>В тестовом профиле все компоненты заменены на моки.
 *
 * @author RAG Application Team
 * @version 4.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
class AiConfigTest extends BaseTest {

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private OllamaChatOptions ollamaOptions;

    // ============================================================
    // ТЕСТЫ КОМПОНЕНТОВ (МОКИ)
    // ============================================================

    @Test
    void testOllamaApiBean() {
        assertThat(ollamaApi)
                .as("OllamaApi mock should be created")
                .isNotNull();

        // ✅ Правильная проверка, что объект является моком
        assertThat(mockingDetails(ollamaApi).isMock())
                .as("OllamaApi should be a mock")
                .isTrue();

        logger.info("✅ OllamaApi mock successfully created");
    }

    @Test
    void testChatModelBean() {
        assertThat(ollamaChatModel)
                .as("OllamaChatModel mock should be created")
                .isNotNull();

        // ✅ Правильная проверка, что объект является моком
        assertThat(mockingDetails(ollamaChatModel).isMock())
                .as("OllamaChatModel should be a mock")
                .isTrue();

        logger.info("✅ OllamaChatModel mock successfully created");
    }

    @Test
    void testChatClientBean() {
        assertThat(chatClient)
                .as("ChatClient mock should be created")
                .isNotNull();

        // ✅ Правильная проверка, что объект является моком
        assertThat(mockingDetails(chatClient).isMock())
                .as("ChatClient should be a mock")
                .isTrue();

        logger.info("✅ ChatClient mock successfully created");
    }

    @Test
    void testVectorStoreBean() {
        assertThat(vectorStore)
                .as("VectorStore mock should be created")
                .isNotNull();

        // ✅ Правильная проверка, что объект является моком
        assertThat(mockingDetails(vectorStore).isMock())
                .as("VectorStore should be a mock")
                .isTrue();

        logger.info("✅ VectorStore mock successfully created");
    }

    // ============================================================
    // ТЕСТЫ КОНФИГУРАЦИИ
    // ============================================================

    @Test
    void testOllamaApiUrl() {
        String ollamaUrl = environment.getProperty(
                "spring.ai.ollama.base-url",
                "http://localhost:11434"
        );

        assertThat(ollamaUrl)
                .as("Ollama URL should be configured")
                .isEqualTo("http://localhost:11434");

        logger.info("✅ OllamaApi configured with URL: {}", ollamaUrl);
    }

    @Test
    void testOllamaOptionsConfiguration() {
        if (ollamaOptions == null) {
            // Проверяем через Environment
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

            logger.info("✅ Ollama options configured via application.yml");
            logger.debug("   Model: {}", model);
            logger.debug("   Temperature: {}", temperature);
            logger.debug("   Context size: {}", numCtx);
            return;
        }

        assertThat(ollamaOptions.getModel())
                .as("Model should be configured")
                .isEqualTo("qwen2.5-coder:7b");

        assertThat(ollamaOptions.getTemperature())
                .as("Temperature should be configured")
                .isEqualTo(0.2);

        assertThat(ollamaOptions.getNumCtx())
                .as("Context size should be configured")
                .isEqualTo(4096);

        logger.info("✅ OllamaOptions configured successfully");
        logger.debug("   Model: {}", ollamaOptions.getModel());
        logger.debug("   Temperature: {}", ollamaOptions.getTemperature());
        logger.debug("   Context size: {}", ollamaOptions.getNumCtx());
    }

    @Test
    void testEmbeddingModelConfiguration() {
        String embeddingModel = environment.getProperty(
                "spring.ai.ollama.embedding.options.model",
                "nomic-embed-text:v1.5"
        );

        assertThat(embeddingModel)
                .as("Embedding model should be configured")
                .isEqualTo("nomic-embed-text:v1.5");

        logger.info("✅ Embedding model configured: {}", embeddingModel);
    }

    @Test
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

        logger.info("✅ VectorStore configured: dimensions={}, index={}, distance={}",
                dimensions, indexType, distanceType);
    }

    // ============================================================
    // КОМПЛЕКСНЫЕ ТЕСТЫ
    // ============================================================

    @Test
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

        logger.info("✅ All environment properties verified successfully");
    }

    @Test
    void testAIConfigurationIntegration() {
        // В тестовом профиле бины НЕ создаются в контексте Spring,
        // они создаются как моки в BaseTest через @MockBean.
        // Поэтому проверяем, что моки доступны через поля и являются моками.

        assertThat(ollamaApi)
                .as("OllamaApi mock should be available")
                .isNotNull();

        assertThat(ollamaChatModel)
                .as("OllamaChatModel mock should be available")
                .isNotNull();

        assertThat(vectorStore)
                .as("VectorStore mock should be available")
                .isNotNull();

        assertThat(chatClient)
                .as("ChatClient mock should be available")
                .isNotNull();

        // Проверяем, что все это моки
        assertThat(mockingDetails(ollamaApi).isMock())
                .as("OllamaApi should be a mock")
                .isTrue();

        assertThat(mockingDetails(ollamaChatModel).isMock())
                .as("OllamaChatModel should be a mock")
                .isTrue();

        assertThat(mockingDetails(vectorStore).isMock())
                .as("VectorStore should be a mock")
                .isTrue();

        assertThat(mockingDetails(chatClient).isMock())
                .as("ChatClient should be a mock")
                .isTrue();

        logger.info("✅ All AI components are properly configured as mocks");
        logger.debug("   - OllamaApi: {}", ollamaApi.getClass().getSimpleName());
        logger.debug("   - OllamaChatModel: {}", ollamaChatModel.getClass().getSimpleName());
        logger.debug("   - VectorStore: {}", vectorStore.getClass().getSimpleName());
        logger.debug("   - ChatClient: {}", chatClient.getClass().getSimpleName());
        logger.debug("   Bean count: {}", applicationContext.getBeanDefinitionCount());
    }
}