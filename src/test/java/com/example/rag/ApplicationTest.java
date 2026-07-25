package com.example.rag;

import org.junit.jupiter.api.Test;
import com.example.rag.support.BaseTest;
import com.example.rag.support.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки загрузки Spring контекста.
 * Проверяет, что все бины и моки созданы корректно.
 *
 * <p>Данный тест является критическим, так как проверяет базовую
 * работоспособность Spring контекста. Если этот тест падает,
 * остальные тесты также не будут работать.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
class ApplicationTest extends BaseTest {

    /**
     * Проверяет, что Spring контекст загружается успешно.
     *
     * <p>Тест выполняет следующие проверки:
     * <ul>
     *   <li>Загрузка всех необходимых бинов</li>
     *   <li>Создание всех моков</li>
     *   <li>Приложение доступно для использования</li>
     * </ul>
     *
     * <p>Время выполнения теста измеряется с помощью {@link TestUtils}.
     */
    @Test
    void contextLoads() {
        logTestStart("Loading Spring context");

        TestUtils.measureExecutionTime("Context loading", () -> {
            assertAllBeansLoaded();

            assertThat(application)
                    .as("Application bean should not be null")
                    .isNotNull();

            assertThat(applicationContext)
                    .as("ApplicationContext should not be null")
                    .isNotNull();

            // Проверяем, что все моки созданы
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

            assertThat(embeddingModel)
                    .as("EmbeddingModel mock should be created")
                    .isNotNull();

            // Проверяем, что DataSource доступен (если есть)
            if (dataSource != null) {
                assertThat(dataSource)
                        .as("DataSource should be available")
                        .isNotNull();
            }

            // Проверяем активные профили
            String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
            assertThat(activeProfiles)
                    .as("Active profiles should contain 'test'")
                    .contains("test");
        });

        logger.info("✅ Spring context loaded successfully!");
        logger.info("📊 Bean count: {}", applicationContext.getBeanDefinitionCount());
        logger.info("📊 Active profiles: {}", String.join(", ", applicationContext.getEnvironment().getActiveProfiles()));
        logger.info("📊 All mocks created successfully:");
        logger.debug("   - OllamaApi: {}", ollamaApi.getClass().getSimpleName());
        logger.debug("   - OllamaChatModel: {}", ollamaChatModel.getClass().getSimpleName());
        logger.debug("   - VectorStore: {}", vectorStore.getClass().getSimpleName());
        logger.debug("   - ChatClient: {}", chatClient.getClass().getSimpleName());
        logger.debug("   - EmbeddingModel: {}", embeddingModel.getClass().getSimpleName());
        if (dataSource != null) {
            logger.debug("   - DataSource: {}", dataSource.getClass().getSimpleName());
        }

        logTestSuccess("Spring context loaded successfully");
    }

    /**
     * Проверяет, что профиль 'test' активен.
     */
    @Test
    void testActiveProfile() {
        logTestStart("Checking active profile");

        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        assertThat(activeProfiles)
                .as("Active profiles should contain 'test'")
                .contains("test");

        logger.info("✅ Active profiles: {}", String.join(", ", activeProfiles));

        logTestSuccess("Active profile verified");
    }

    /**
     * Проверяет, что приложение имеет правильное имя.
     */
    @Test
    void testApplicationName() {
        logTestStart("Checking application name");

        String appName = applicationContext.getApplicationName();
        assertThat(appName)
                .as("Application name should not be null")
                .isNotNull();

        logger.info("✅ Application name: {}", appName);

        logTestSuccess("Application name verified");
    }

    /**
     * Проверяет, что все необходимые свойства загружены.
     */
    @Test
    void testRequiredProperties() {
        logTestStart("Checking required properties");

        // Проверяем DataSource URL
        String dbUrl = environment.getProperty("spring.datasource.url");
        assertThat(dbUrl)
                .as("Database URL should be configured")
                .isNotNull()
                .contains("postgresql")
                .doesNotContain("h2");

        // Проверяем Ollama URL
        String ollamaUrl = environment.getProperty("spring.ai.ollama.base-url");
        assertThat(ollamaUrl)
                .as("Ollama URL should be configured")
                .isNotNull()
                .isEqualTo("http://localhost:11434");

        // Проверяем модель
        String model = environment.getProperty("spring.ai.ollama.chat.options.model");
        assertThat(model)
                .as("Model should be configured")
                .isNotNull()
                .isEqualTo("qwen2.5-coder:7b");

        logger.info("✅ Required properties verified:");
        logger.debug("   Database URL: {}", dbUrl);
        logger.debug("   Ollama URL: {}", ollamaUrl);
        logger.debug("   Model: {}", model);

        logTestSuccess("Required properties verified");
    }
}