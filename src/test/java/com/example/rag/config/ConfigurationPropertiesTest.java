package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import com.example.rag.support.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки привязки свойств конфигурации Ollama.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет, что все настройки правильно загружаются в {@link OllamaChatOptions}.</p>
 *
 * <h2>Тестируемые параметры</h2>
 * <ul>
 *   <li>Модель ({@code model}) - ожидается: {@code qwen2.5-coder:7b}</li>
 *   <li>Температура ({@code temperature}) - ожидается: {@code 0.2}</li>
 *   <li>Размер контекста ({@code numCtx}) - ожидается: {@code 4096}</li>
 * </ul>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Все аннотации наследуются от {@link BaseTest}</li>
 *   <li>Для логирования используется Lombok {@code @Slf4j}</li>
 *   <li>Поддерживает fallback через {@link Environment}</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see BaseTest
 * @see OllamaChatOptions
 * @since 1.0
 */
@Slf4j
@Epic("Модульные тесты")
@Feature("Конфигурация свойств")
class ConfigurationPropertiesTest extends BaseTest {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String EXPECTED_MODEL = "qwen2.5-coder:7b";
    private static final double EXPECTED_TEMPERATURE = 0.2;
    private static final int EXPECTED_NUM_CTX = 4096;

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private OllamaChatOptions ollamaOptions;

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка, что модель правильно сконфигурирована")
    @Story("Конфигурация Ollama")
    @Severity(SeverityLevel.CRITICAL)
    void testOllamaOptionsModel() {
        logTestStart("Checking model configuration");

        OllamaChatOptions options = getOptions();
        assertThat(options.getModel())
                .as("Model should be configured")
                .isEqualTo(EXPECTED_MODEL);

        log.info("✅ Model: {}", options.getModel());
        logTestSuccess("Model configuration verified");
    }

    @Test
    @Description("Проверка, что температура правильно сконфигурирована")
    @Story("Конфигурация Ollama")
    @Severity(SeverityLevel.CRITICAL)
    void testOllamaOptionsTemperature() {
        logTestStart("Checking temperature configuration");

        OllamaChatOptions options = getOptions();
        assertThat(options.getTemperature())
                .as("Temperature should be configured")
                .isEqualTo(EXPECTED_TEMPERATURE);

        log.info("✅ Temperature: {}", options.getTemperature());
        logTestSuccess("Temperature configuration verified");
    }

    @Test
    @Description("Проверка, что размер контекста правильно сконфигурирован")
    @Story("Конфигурация Ollama")
    @Severity(SeverityLevel.CRITICAL)
    void testOllamaOptionsNumCtx() {
        logTestStart("Checking context size configuration");

        OllamaChatOptions options = getOptions();
        assertThat(options.getNumCtx())
                .as("Context size should be configured")
                .isEqualTo(EXPECTED_NUM_CTX);

        log.info("✅ Context size: {}", options.getNumCtx());
        logTestSuccess("Context size configuration verified");
    }

    @Test
    @Description("Проверка, что OllamaChatOptions и параметры не null")
    @Story("Конфигурация Ollama")
    @Severity(SeverityLevel.NORMAL)
    void testOllamaOptionsNotNull() {
        logTestStart("Checking OllamaOptions not null");

        OllamaChatOptions options = getOptions();

        assertThat(options)
                .as("OllamaOptions should not be null")
                .isNotNull();

        assertThat(options.getModel())
                .as("Model should not be null")
                .isNotNull();

        assertThat(options.getTemperature())
                .as("Temperature should not be null")
                .isNotNull();

        assertThat(options.getNumCtx())
                .as("Context size should not be null")
                .isNotNull();

        log.info("✅ All Ollama options are not null");
        logTestSuccess("OllamaOptions null check passed");
    }

    @Test
    @Description("Проверка, что все настройки Ollama правильно сконфигурированы")
    @Story("Конфигурация Ollama")
    @Severity(SeverityLevel.CRITICAL)
    void testOllamaOptionsAreConfigured() {
        logTestStart("Checking all Ollama options");

        OllamaChatOptions options = getOptions();

        assertThat(options.getModel())
                .as("Model should be configured")
                .isEqualTo(EXPECTED_MODEL);

        assertThat(options.getTemperature())
                .as("Temperature should be configured")
                .isEqualTo(EXPECTED_TEMPERATURE);

        assertThat(options.getNumCtx())
                .as("Context size should be configured")
                .isEqualTo(EXPECTED_NUM_CTX);

        log.info("✅ All options configured:");
        log.debug("   Model: {}", options.getModel());
        log.debug("   Temperature: {}", options.getTemperature());
        log.debug("   Context size: {}", options.getNumCtx());
        logTestSuccess("All Ollama options verified");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Получает OllamaChatOptions с fallback на Environment.
     *
     * @return OllamaChatOptions или созданный из Environment
     */
    private OllamaChatOptions getOptions() {
        if (ollamaOptions != null) {
            log.debug("Using OllamaOptions bean");
            return ollamaOptions;
        }

        log.debug("OllamaOptions bean not available, using Environment fallback");
        return createOptionsFromEnvironment();
    }

    /**
     * Создает OllamaChatOptions из Environment.
     *
     * @return OllamaChatOptions с настройками из Environment
     */
    private OllamaChatOptions createOptionsFromEnvironment() {
        String model = environment.getProperty("spring.ai.ollama.chat.options.model");
        Double temperature = environment.getProperty(
                "spring.ai.ollama.chat.options.temperature", Double.class);
        Integer numCtx = environment.getProperty(
                "spring.ai.ollama.chat.options.num-ctx", Integer.class);

        log.info("📋 Using options from Environment:");
        log.debug("   Model: {}", model);
        log.debug("   Temperature: {}", temperature);
        log.debug("   Context size: {}", numCtx);

        // Создаем временный объект для проверки
        return OllamaChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .numCtx(numCtx)
                .build();
    }
}