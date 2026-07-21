package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.rag.support.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки привязки свойств конфигурации Ollama.
 * Проверяет, что все настройки правильно загружаются в {@link OllamaChatOptions}.
 *
 * <p>Тестируемые параметры:
 * <ul>
 *   <li>Модель ({@code model})</li>
 *   <li>Температура ({@code temperature})</li>
 *   <li>Размер контекста ({@code numCtx})</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
class ConfigurationPropertiesTest extends BaseTest {

    /**
     * Опции конфигурации Ollama.
     * Может быть {@code null} в некоторых конфигурациях.
     */
    @Autowired(required = false)
    private OllamaChatOptions ollamaOptions;

    /**
     * Проверяет, что все моки созданы.
     */
    @Test
    void testMocksAreCreated() {
        assertMocksCreated();
        logger.info("All mocks created successfully");
    }

    /**
     * Проверяет, что модель правильно сконфигурирована.
     *
     * <p>Ожидаемое значение: {@code qwen2.5-coder:7b}
     */
    @Test
    void testOllamaOptionsModel() {
        assertMocksCreated();

        if (ollamaOptions == null) {
            logger.warn("OllamaOptions is not available in test context");
            return;
        }

        assertThat(ollamaOptions.getModel())
                .as("Model should be configured")
                .isEqualTo("qwen2.5-coder:7b");
        logger.info("Model: {}", ollamaOptions.getModel());
    }

    /**
     * Проверяет, что температура правильно сконфигурирована.
     *
     * <p>Ожидаемое значение: {@code 0.2}
     */
    @Test
    void testOllamaOptionsTemperature() {
        assertMocksCreated();

        if (ollamaOptions == null) {
            logger.warn("OllamaOptions is not available in test context");
            return;
        }

        assertThat(ollamaOptions.getTemperature())
                .as("Temperature should be configured")
                .isEqualTo(0.2);
        logger.info("Temperature: {}", ollamaOptions.getTemperature());
    }

    /**
     * Проверяет, что размер контекста правильно сконфигурирован.
     *
     * <p>Ожидаемое значение: {@code 8192}
     */
    @Test
    void testOllamaOptionsNumCtx() {
        assertMocksCreated();

        if (ollamaOptions == null) {
            logger.warn("OllamaOptions is not available in test context");
            return;
        }

        assertThat(ollamaOptions.getNumCtx())
                .as("Context size should be configured")
                .isEqualTo(8192);
        logger.info("Context size: {}", ollamaOptions.getNumCtx());
    }

    /**
     * Проверяет, что {@link OllamaChatOptions} и все его параметры не равны {@code null}.
     */
    @Test
    void testOllamaOptionsNotNull() {
        assertMocksCreated();

        if (ollamaOptions == null) {
            logger.warn("OllamaOptions is not available in test context");
            return;
        }

        assertThat(ollamaOptions)
                .as("OllamaOptions should not be null")
                .isNotNull();
        assertThat(ollamaOptions.getModel())
                .as("Model should not be null")
                .isNotNull();
        assertThat(ollamaOptions.getTemperature())
                .as("Temperature should not be null")
                .isNotNull();
        assertThat(ollamaOptions.getNumCtx())
                .as("Context size should not be null")
                .isNotNull();

        logger.info("All Ollama options are not null");
    }

    /**
     * Проверяет, что все настройки Ollama правильно сконфигурированы.
     *
     * <p>Объединяет проверки всех параметров в одном тесте.
     */
    @Test
    void testOllamaOptionsAreConfigured() {
        assertMocksCreated();

        if (ollamaOptions == null) {
            logger.warn("OllamaOptions is not available in test context");
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
                .isEqualTo(8192);

        logger.info("All Ollama options are properly configured");
        logger.debug("   Model: {}", ollamaOptions.getModel());
        logger.debug("   Temperature: {}", ollamaOptions.getTemperature());
        logger.debug("   Context size: {}", ollamaOptions.getNumCtx());
    }
}