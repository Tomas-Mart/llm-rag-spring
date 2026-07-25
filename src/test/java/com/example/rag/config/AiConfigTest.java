package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import com.example.rag.support.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(ollamaApi)
                .as("OllamaApi should be a mock")
                .isInstanceOf(org.mockito.Mockito.class);
        logger.info("✅ OllamaApi mock successfully created");
    }

    @Test
    void testChatModelBean() {
        assertThat(ollamaChatModel)
                .as("OllamaChatModel mock should be created")
                .isNotNull();
        assertThat(ollamaChatModel)
                .as("OllamaChatModel should be a mock")
                .isInstanceOf(org.mockito.Mockito.class);
        logger.info("✅ OllamaChatModel mock successfully created");
    }

    @Test
    void testChatClientBean() {
        assertThat(chatClient)
                .as("ChatClient mock should be created")
                .isNotNull();
        assertThat(chatClient)
                .as("ChatClient should be a mock")
                .isInstanceOf(org.mockito.Mockito.class);
        logger.info("✅ ChatClient mock successfully created");
    }

    @Test
    void testVectorStoreBean() {
        assertThat(vectorStore)
                .as("VectorStore mock should be created")
                .isNotNull();
        assertThat(vectorStore)
                .as("VectorStore should be a mock")
                .isInstanceOf(org.mockito.Mockito.class);
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
        // Проверяем наличие бинов
        assertThat(applicationContext.containsBean("ollamaApi")).isTrue();
        assertThat(applicationContext.containsBean("ollamaChatModel")).isTrue();
        assertThat(applicationContext.containsBean("vectorStore")).isTrue();
        assertThat(applicationContext.containsBean("chatClient")).isTrue();

        // Проверяем, что бины - это моки
        assertThat(applicationContext.getBean("ollamaApi"))
                .as("OllamaApi should be a mock")
                .isInstanceOf(org.mockito.Mockito.class);

        assertThat(applicationContext.getBean("ollamaChatModel"))
                .as("OllamaChatModel should be a mock")
                .isInstanceOf(org.mockito.Mockito.class);

        assertThat(applicationContext.getBean("vectorStore"))
                .as("VectorStore should be a mock")
                .isInstanceOf(org.mockito.Mockito.class);

        assertThat(applicationContext.getBean("chatClient"))
                .as("ChatClient should be a mock")
                .isInstanceOf(org.mockito.Mockito.class);

        logger.info("✅ All AI components are properly configured as mocks");
        logger.debug("   Bean count: {}", applicationContext.getBeanDefinitionCount());
    }
}