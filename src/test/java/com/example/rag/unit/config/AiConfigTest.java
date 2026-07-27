package com.example.rag.unit.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import com.example.rag.support.BaseTest;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

@Slf4j
class AiConfigTest extends BaseTest {

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private OllamaChatOptions ollamaOptions;

    @Test
    void testOllamaApiBean() {
        assertThat(ollamaApi).isNotNull();
        assertThat(mockingDetails(ollamaApi).isMock()).isTrue();
        log.info("✅ OllamaApi mock created");
    }

    @Test
    void testChatModelBean() {
        assertThat(ollamaChatModel).isNotNull();
        assertThat(mockingDetails(ollamaChatModel).isMock()).isTrue();
        log.info("✅ OllamaChatModel mock created");
    }

    @Test
    void testChatClientBean() {
        assertThat(chatClient).isNotNull();
        assertThat(mockingDetails(chatClient).isMock()).isTrue();
        log.info("✅ ChatClient mock created");
    }

    @Test
    void testVectorStoreBean() {
        assertThat(vectorStore).isNotNull();
        assertThat(mockingDetails(vectorStore).isMock()).isTrue();
        log.info("✅ VectorStore mock created");
    }

    @Test
    void testOllamaApiUrl() {
        var url = environment.getProperty("spring.ai.ollama.base-url", "http://localhost:11434");
        assertThat(url).isEqualTo("http://localhost:11434");
        log.info("✅ Ollama URL: {}", url);
    }

    @Test
    void testOllamaOptionsConfiguration() {
        if (ollamaOptions != null) {
            verifyOptionsFromBean();
        } else {
            verifyOptionsFromEnvironment();
        }
    }

    @Test
    void testEmbeddingModelConfiguration() {
        var model = environment.getProperty("spring.ai.ollama.embedding.options.model", "nomic-embed-text:v1.5");
        assertThat(model).isEqualTo("nomic-embed-text:v1.5");
        log.info("✅ Embedding model: {}", model);
    }

    @Test
    void testVectorStoreConfiguration() {
        var dimensions = environment.getProperty("spring.ai.vectorstore.pgvector.dimensions", Integer.class);
        assertThat(dimensions).isEqualTo(768);

        var indexType = environment.getProperty("spring.ai.vectorstore.pgvector.index-type");
        assertThat(indexType).isEqualTo("HNSW");

        log.info("✅ VectorStore: dims={}, index={}", dimensions, indexType);
    }

    @Test
    void testDataSourceConfiguration() {
        var url = environment.getProperty("spring.datasource.url");
        assertThat(url)
                .contains("h2")  // ✅ Исправлено: модульные тесты используют H2
                .doesNotContain("postgresql");

        var driver = environment.getProperty("spring.datasource.driver-class-name");
        assertThat(driver).isEqualTo("org.h2.Driver");  // ✅ Исправлено на H2

        log.info("✅ DataSource: H2 for unit tests");
    }

    @Test
    void testAIConfigurationIntegration() {
        // ✅ Проверка всех бинов в одной цепочке
        assertThat(ollamaApi)
                .as("OllamaApi mock should be created")
                .isNotNull();

        assertThat(ollamaChatModel)
                .as("OllamaChatModel mock should be created")
                .isNotNull();

        assertThat(vectorStore)
                .as("VectorStore mock should be created")
                .isNotNull();

        assertThat(chatClient)
                .as("ChatClient mock should be created")
                .isNotNull();

        // ✅ Проверка, что все бины являются моками
        assertThat(mockingDetails(ollamaApi).isMock())
                .as("All AI components should be mocks")
                .isTrue();

        assertThat(mockingDetails(ollamaChatModel).isMock())
                .as("All AI components should be mocks")
                .isTrue();

        assertThat(mockingDetails(vectorStore).isMock())
                .as("All AI components should be mocks")
                .isTrue();

        assertThat(mockingDetails(chatClient).isMock())
                .as("All AI components should be mocks")
                .isTrue();

        // ✅ Проверка H2 в одной цепочке
        assertThat(environment.getProperty("spring.datasource.url"))
                .as("Database URL should be H2 for unit tests")
                .isNotNull()
                .contains("h2")
                .doesNotContain("postgresql");

        log.info("✅ AI components configured as mocks");
        log.info("✅ Using H2 for unit tests");
    }

    private void verifyOptionsFromBean() {
        assertThat(ollamaOptions)
                .isNotNull()
                .satisfies(options -> {
                    assertThat(options.getModel()).isEqualTo("qwen2.5-coder:7b");
                    assertThat(options.getTemperature()).isEqualTo(0.2);
                    assertThat(options.getNumCtx()).isEqualTo(4096);
                });

        log.info("✅ Options from bean: model={}, temp={}, ctx={}",
                ollamaOptions.getModel(),
                ollamaOptions.getTemperature(),
                ollamaOptions.getNumCtx());
    }

    private void verifyOptionsFromEnvironment() {
        var model = environment.getProperty("spring.ai.ollama.chat.options.model");
        var temperature = environment.getProperty("spring.ai.ollama.chat.options.temperature", Double.class);
        var numCtx = environment.getProperty("spring.ai.ollama.chat.options.num-ctx", Integer.class);

        assertThat(model).isEqualTo("qwen2.5-coder:7b");
        assertThat(temperature).isEqualTo(0.2);
        assertThat(numCtx).isEqualTo(4096);

        log.info("✅ Options from environment: model={}, temp={}, ctx={}", model, temperature, numCtx);
    }
}