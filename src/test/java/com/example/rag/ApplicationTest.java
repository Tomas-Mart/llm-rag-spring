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
        TestUtils.measureExecutionTime("Context loading", () -> {
            assertAllBeansLoaded();

            assertThat(application)
                    .as("Application bean should not be null")
                    .isNotNull();
        });

        logger.info("Spring context loaded successfully!");
        logger.info("All mocks created successfully:");
        logger.info("   - OllamaApi: {}", ollamaApi.getClass().getSimpleName());
        logger.info("   - OllamaChatModel: {}", ollamaChatModel.getClass().getSimpleName());
        logger.info("   - VectorStore: {}", vectorStore.getClass().getSimpleName());
    }
}