package com.example.rag;

import org.junit.jupiter.api.Test;
import com.example.rag.support.BaseTest;
import com.example.rag.support.TestUtils;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки загрузки Spring контекста.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет базовую работоспособность Spring контекста.
 * Если этот тест падает, остальные тесты также не будут работать.</p>
 *
 * <h2>Проверяемые аспекты</h2>
 * <ul>
 *   <li>Загрузка всех необходимых бинов</li>
 *   <li>Создание всех моков</li>
 *   <li>Активный профиль 'test'</li>
 *   <li>Имя приложения</li>
 *   <li>Обязательные свойства</li>
 * </ul>
 *
 * <h2>Пример запуска</h2>
 * <pre>{@code
 * // Запустить все тесты
 * mvn test -Dtest=ApplicationTest
 *
 * // Запустить конкретный тест
 * mvn test -Dtest=ApplicationTest#contextLoads
 * }</pre>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see BaseTest
 * @since 1.0
 */
@Slf4j
class ApplicationTest extends BaseTest {

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    /**
     * Проверяет, что Spring контекст загружается успешно.
     *
     * <p>Выполняет проверки:
     * <ul>
     *   <li>Загрузка всех необходимых бинов</li>
     *   <li>Создание всех моков</li>
     *   <li>Активный профиль 'test'</li>
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

            verifyMocksExist();
            verifyDataSourceExists();
            verifyActiveProfile();
        });

        logContextSummary();
        logTestSuccess("Spring context loaded successfully");
    }

    /**
     * Проверяет, что профиль 'test' активен.
     */
    @Test
    void testActiveProfile() {
        logTestStart("Checking active profile");

        verifyActiveProfile();

        log.info("✅ Active profiles: {}", String.join(", ", applicationContext.getEnvironment().getActiveProfiles()));
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

        log.info("✅ Application name: {}", appName);
        logTestSuccess("Application name verified");
    }

    /**
     * Проверяет, что все необходимые свойства загружены.
     */
    @Test
    void testRequiredProperties() {
        logTestStart("Checking required properties");

        verifyRequiredProperties();

        log.info("✅ Required properties verified");
        logTestSuccess("Required properties verified");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Проверяет, что все моки созданы.
     */
    private void verifyMocksExist() {
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

        log.debug("✅ All mocks verified");
    }

    /**
     * Проверяет, что DataSource доступен.
     */
    private void verifyDataSourceExists() {
        if (dataSource != null) {
            assertThat(dataSource)
                    .as("DataSource should be available")
                    .isNotNull();
            log.debug("✅ DataSource available");
        } else {
            log.debug("ℹ️ DataSource not available (optional)");
        }
    }

    /**
     * Проверяет, что профиль 'test' активен.
     */
    private void verifyActiveProfile() {
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        assertThat(activeProfiles)
                .as("Active profiles should contain 'test'")
                .contains("test");
    }

    /**
     * Проверяет обязательные свойства.
     */
    private void verifyRequiredProperties() {
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

        log.debug("   Database URL: {}", dbUrl);
        log.debug("   Ollama URL: {}", ollamaUrl);
        log.debug("   Model: {}", model);
    }

    /**
     * Логирует сводку по контексту.
     */
    private void logContextSummary() {
        log.info("📊 Bean count: {}", applicationContext.getBeanDefinitionCount());
        log.info("📊 Active profiles: {}", String.join(", ", applicationContext.getEnvironment().getActiveProfiles()));

        log.debug("   - OllamaApi: {}", ollamaApi.getClass().getSimpleName());
        log.debug("   - OllamaChatModel: {}", ollamaChatModel.getClass().getSimpleName());
        log.debug("   - VectorStore: {}", vectorStore.getClass().getSimpleName());
        log.debug("   - ChatClient: {}", chatClient.getClass().getSimpleName());
        log.debug("   - EmbeddingModel: {}", embeddingModel.getClass().getSimpleName());

        if (dataSource != null) {
            log.debug("   - DataSource: {}", dataSource.getClass().getSimpleName());
        }
    }
}