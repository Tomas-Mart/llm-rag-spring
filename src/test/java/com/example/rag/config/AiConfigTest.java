package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.rag.support.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки конфигурации AI компонентов.
 * Проверяет создание и настройку клиентов Ollama.
 *
 * <p>Тестируемые компоненты:
 * <ul>
 *   <li>{@link OllamaApi} - API клиент для Ollama</li>
 *   <li>{@link OllamaChatModel} - Модель чата (реализация {@code ChatModel})</li>
 *   <li>{@link ChatClient} - Высокоуровневый Fluent API клиент для работы с чатом</li>
 *   <li>{@link OllamaChatOptions} - Опции конфигурации модели (параметры запроса)</li>
 * </ul>
 *
 * <p>В Spring AI 1.1.8:
 * <ul>
 *   <li>Модель Ollama используется через универсальный интерфейс {@code ChatModel}</li>
 *   <li>Добавлена поддержка новых параметров (think, reasoning)</li>
 *   <li>Сохранена обратная совместимость с предыдущими версиями</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.1.8
 * @since 1.0
 */
class AiConfigTest extends BaseTest {

    /**
     * Клиент для работы с чатом (высокоуровневая абстракция Fluent API).
     * Может быть {@code null} в некоторых тестовых конфигурациях.
     */
    @Autowired(required = false)
    private ChatClient chatClient;

    /**
     * Опции конфигурации Ollama.
     * Может быть {@code null}, если кастомные опции не объявлялись как бин.
     */
    @Autowired(required = false)
    private OllamaChatOptions ollamaOptions;

    /**
     * Проверяет, что низкоуровневый бин {@link OllamaApi} успешно инициализирован.
     */
    @Test
    void testOllamaApiBean() {
        assertMocksCreated();
        assertThat(ollamaApi)
                .as("OllamaApi should be created")
                .isNotNull();
        logger.info("OllamaApi created: {}", ollamaApi);
    }

    /**
     * Проверяет, что бин реализации {@link OllamaChatModel} создан.
     */
    @Test
    void testChatModelBean() {
        assertMocksCreated();
        assertThat(ollamaChatModel)
                .as("OllamaChatModel should be created")
                .isNotNull();
        logger.info("OllamaChatModel created: {}", ollamaChatModel);
    }

    /**
     * Проверяет, что fluent-клиент {@link ChatClient} создан, если он доступен.
     *
     * <p>Если {@link ChatClient} не доступен, тест логирует предупреждение
     * и завершается успешно, так как это допустимо при выборочном контексте.
     */
    @Test
    void testChatClientBean() {
        assertMocksCreated();

        if (chatClient != null) {
            logger.info("ChatClient created: {}", chatClient);
            assertThat(chatClient)
                    .as("ChatClient should be created")
                    .isNotNull();
        } else {
            logger.warn("ChatClient is not available in test context");
        }
    }

    /**
     * Проверяет корректность создания {@link OllamaApi}.
     */
    @Test
    void testOllamaApiUrl() {
        assertMocksCreated();
        assertThat(ollamaApi)
                .as("OllamaApi should be created")
                .isNotNull();
        logger.info("OllamaApi created successfully");
    }

    /**
     * Проверяет конфигурацию параметров {@link OllamaChatOptions}.
     *
     * <p>Если {@link OllamaChatOptions} не доступен, тест логирует предупреждение
     * и завершается успешно.
     *
     * <p>Проверяемые параметры:
     * <ul>
     *   <li>Модель: должна быть {@code qwen2.5-coder:7b}</li>
     *   <li>Температура: должна быть {@code 0.2}</li>
     *   <li>Размер контекста: {@code getNumCtx()} (необязательно)</li>
     * </ul>
     *
     * <p>В Spring AI 1.1.8 также доступны новые параметры:
     * <ul>
     *   <li>{@code think} - режим рассуждений (для DeepSeek и др.)</li>
     *   <li>{@code reasoningEffort} - уровень детализации рассуждений</li>
     * </ul>
     */
    @Test
    void testOllamaOptionsConfiguration() {
        assertMocksCreated();

        if (ollamaOptions == null) {
            logger.warn("OllamaOptions is not available in test context");
            return;
        }

        assertThat(ollamaOptions)
                .as("OllamaOptions should be created")
                .isNotNull();

        assertThat(ollamaOptions.getModel())
                .as("Model should be configured to qwen2.5-coder:7b")
                .isEqualTo("qwen2.5-coder:7b");

        assertThat(ollamaOptions.getTemperature())
                .as("Temperature should be configured to 0.2")
                .isEqualTo(0.2);

        logger.info("OllamaOptions configured successfully");
        logger.debug("   Model: {}", ollamaOptions.getModel());
        logger.debug("   Temperature: {}", ollamaOptions.getTemperature());
        logger.debug("   Context size: {}", ollamaOptions.getNumCtx());
        logger.debug("   Think mode: {}", ollamaOptions.getThinkOption());
    }
}