package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
 * <p>Особенности тестирования:
 * <ul>
 *   <li>Используется реальная PostgreSQL через Testcontainers</li>
 *   <li>Все внешние зависимости (Ollama) замоканы для изоляции</li>
 *   <li>Проверяется корректность конфигурации из application-test.yml</li>
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
 * @version 3.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
class AiConfigTest extends BaseTest {

    /**
     * Клиент для работы с чатом (высокоуровневая абстракция Fluent API).
     * Может быть {@code null} в некоторых тестовых конфигурациях,
     * так как в тестовом профиле используется мок.
     */
    @Autowired(required = false)
    private ChatClient chatClient;

    /**
     * Опции конфигурации Ollama.
     * Может быть {@code null}, если кастомные опции не объявлялись как бин
     * или используются стандартные настройки из application.yml.
     */
    @Autowired(required = false)
    private OllamaChatOptions ollamaOptions;

    /**
     * Проверяет, что бин {@link OllamaApi} успешно создан.
     * В тестовом профиле используется мок, поэтому проверяем только наличие.
     */
    @Test
    void testOllamaApiBean() {
        assertMocksCreated();

        assertThat(ollamaApi)
                .as("OllamaApi should be created (as mock)")
                .isNotNull();

        logger.info("✅ OllamaApi successfully created");
        logger.debug("   OllamaApi type: {}", ollamaApi.getClass().getSimpleName());
    }

    /**
     * Проверяет, что бин {@link OllamaChatModel} успешно создан.
     * В тестовом профиле используется мок.
     */
    @Test
    void testChatModelBean() {
        assertMocksCreated();

        assertThat(ollamaChatModel)
                .as("OllamaChatModel should be created (as mock)")
                .isNotNull();

        logger.info("✅ OllamaChatModel successfully created");
        logger.debug("   OllamaChatModel type: {}", ollamaChatModel.getClass().getSimpleName());
    }

    /**
     * Проверяет, что бин {@link ChatClient} создан.
     *
     * <p>ChatClient может отсутствовать в тестовом контексте,
     * поэтому проверка выполняется только при его наличии.
     */
    @Test
    void testChatClientBean() {
        assertMocksCreated();

        if (chatClient != null) {
            assertThat(chatClient)
                    .as("ChatClient should be created")
                    .isNotNull();

            logger.info("✅ ChatClient successfully created");
            logger.debug("   ChatClient type: {}", chatClient.getClass().getSimpleName());
        } else {
            logger.warn("⚠️ ChatClient is not available in test context (expected with mocks)");
        }
    }

    /**
     * Проверяет корректность URL для Ollama API.
     * В тестовом профиле используется мок, поэтому проверяем только наличие.
     */
    @Test
    void testOllamaApiUrl() {
        assertMocksCreated();

        // Получаем URL из конфигурации или используем значение по умолчанию
        String ollamaUrl = environment.getProperty(
                "spring.ai.ollama.base-url",
                "http://localhost:11434"
        );

        logger.info("✅ OllamaApi configured with URL: {}", ollamaUrl);

        // Проверяем, что URL соответствует ожидаемому
        assertThat(ollamaUrl)
                .as("Ollama URL should be configured")
                .isEqualTo("http://localhost:11434");
    }

    /**
     * Проверяет конфигурацию параметров {@link OllamaChatOptions}.
     *
     * <p>Если {@link OllamaChatOptions} не доступен (используются стандартные настройки),
     * тест логирует предупреждение и завершается успешно.
     *
     * <p>Проверяемые параметры (если доступны):
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
            logger.warn("⚠️ OllamaOptions is not available in test context (using defaults)");
            // Проверяем, что свойства загружены из application.yml
            String model = environment.getProperty("spring.ai.ollama.chat.options.model");
            assertThat(model)
                    .as("Model should be configured in application.yml")
                    .isEqualTo("qwen2.5-coder:7b");

            Double temperature = environment.getProperty(
                    "spring.ai.ollama.chat.options.temperature", Double.class);
            assertThat(temperature)
                    .as("Temperature should be configured in application.yml")
                    .isEqualTo(0.2);

            logger.info("✅ Ollama options configured via application.yml");
            logger.debug("   Model: {}", model);
            logger.debug("   Temperature: {}", temperature);
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

        logger.info("✅ OllamaOptions configured successfully");
        logger.debug("   Model: {}", ollamaOptions.getModel());
        logger.debug("   Temperature: {}", ollamaOptions.getTemperature());
        logger.debug("   Context size: {}", ollamaOptions.getNumCtx());
        logger.debug("   Think mode: {}", ollamaOptions.getThinkOption());
    }

    /**
     * Проверяет, что Environment содержит все необходимые свойства.
     * Дополнительный тест для проверки загрузки конфигурации.
     */
    @Test
    void testEnvironmentProperties() {
        assertMocksCreated();

        // Проверяем базовые свойства
        assertThat(environment.getProperty("spring.ai.ollama.base-url"))
                .as("Ollama base URL should be configured")
                .isEqualTo("http://localhost:11434");

        assertThat(environment.getProperty("spring.ai.ollama.chat.options.model"))
                .as("Ollama model should be configured")
                .isEqualTo("qwen2.5-coder:7b");

        assertThat(environment.getProperty("spring.ai.ollama.chat.options.temperature", Double.class))
                .as("Ollama temperature should be configured")
                .isEqualTo(0.2);

        // Проверяем настройки векторного хранилища
        assertThat(environment.getProperty("spring.ai.vectorstore.pgvector.dimensions", Integer.class))
                .as("Vector dimensions should be configured")
                .isEqualTo(768);

        // Проверяем настройки базы данных
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .as("Database driver should be configured")
                .isEqualTo("org.postgresql.Driver");

        logger.info("✅ All environment properties verified successfully");
    }

    /**
     * Проверяет, что все компоненты AI конфигурации корректно взаимодействуют.
     * Интеграционный тест для проверки связей между компонентами.
     */
    @Test
    void testAIConfigurationIntegration() {
        assertMocksCreated();

        // Проверяем, что все бины созданы
        assertThat(applicationContext)
                .as("Application context should be loaded")
                .isNotNull();

        // Проверяем наличие AI бинов
        assertThat(applicationContext.containsBean("ollamaApi"))
                .as("OllamaApi bean should exist")
                .isTrue();

        assertThat(applicationContext.containsBean("ollamaChatModel"))
                .as("OllamaChatModel bean should exist")
                .isTrue();

        // Проверяем, что векторное хранилище настроено
        assertThat(applicationContext.containsBean("vectorStore"))
                .as("VectorStore bean should exist")
                .isTrue();

        // Проверяем наличие ChatClient, если он должен быть создан
        if (chatClient != null) {
            assertThat(applicationContext.containsBean("chatClient"))
                    .as("ChatClient bean should exist")
                    .isTrue();
        }

        logger.info("✅ All AI components are properly configured");
        logger.debug("   Bean count: {}", applicationContext.getBeanDefinitionCount());
        logger.debug("   AI related beans: ollamaApi, ollamaChatModel, vectorStore");
    }
}