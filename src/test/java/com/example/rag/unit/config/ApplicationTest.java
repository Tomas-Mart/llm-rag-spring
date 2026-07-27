package com.example.rag.unit.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import com.example.rag.support.BaseTest;
import com.example.rag.support.TestUtils;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class ApplicationTest extends BaseTest {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        logTestStart("Loading Spring context");

        TestUtils.measureExecutionTime("Context loading", () -> {
            assertAllBeansLoaded();
            verifyActiveProfile();
            verifyRequiredProperties();
        });

        log.info("📊 Bean count: {}", applicationContext.getBeanDefinitionCount());
        log.info("📊 Active profiles: {}", String.join(", ", applicationContext.getEnvironment().getActiveProfiles()));
        logTestSuccess("Spring context loaded successfully");
    }

    @Test
    void testActiveProfile() {
        logTestStart("Checking active profile");
        verifyActiveProfile();
        logTestSuccess("Active profile verified");
    }

    @Test
    void testApplicationName() {
        logTestStart("Checking application name");
        var appName = applicationContext.getApplicationName();
        assertThat(appName).isNotNull();
        log.info("✅ Application name: {}", appName);
        logTestSuccess("Application name verified");
    }

    @Test
    void testRequiredProperties() {
        logTestStart("Checking required properties");
        verifyRequiredProperties();
        logTestSuccess("Required properties verified");
    }

    private void verifyActiveProfile() {
        var profiles = applicationContext.getEnvironment().getActiveProfiles();
        assertThat(profiles)
                .as("Active profiles should contain 'test'")
                .contains("test");
    }

    private void verifyRequiredProperties() {
        var dbUrl = environment.getProperty("spring.datasource.url");
        assertThat(dbUrl)
                .as("Database URL should be configured")
                .isNotNull()
                .contains("h2");  // ✅ Исправлено: модульные тесты используют H2

        var ollamaUrl = environment.getProperty("spring.ai.ollama.base-url");
        assertThat(ollamaUrl)
                .as("Ollama URL should be configured")
                .isNotNull();

        var model = environment.getProperty("spring.ai.ollama.chat.options.model");
        assertThat(model)
                .as("Model should be configured")
                .isNotNull();

        log.info("✅ Database URL: {}", dbUrl);
        log.info("✅ Ollama URL: {}", ollamaUrl);
        log.info("✅ Model: {}", model);
    }

}