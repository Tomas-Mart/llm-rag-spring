package com.example.rag.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.rag.service.RagService;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест для проверки работы {@link RagService}.
 * Проверяет взаимодействие с реальной LLM через Ollama.
 *
 * <p>Тестируемые сценарии:
 * <ul>
 *   <li>Задание вопроса с реальной LLM</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Feature("RAG Сервис")
@Epic("Интеграционные тесты")
class RagServiceIntegrationTest extends BaseIntegrationTestWithContainers {

    /**
     * Сервис RAG для работы с вопросами.
     */
    @Autowired
    private RagService ragService;

    /**
     * Проверяет задание вопроса с реальной LLM через Ollama.
     */
    @Test
    @Description("Тест проверяет возможность задать вопрос и получить ответ от реальной LLM через Ollama")
    @Story("Работа с вопросами")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-123")
    void testAskQuestionWithRealOllama() {
        // Пропускаем если Ollama не доступен
        if (!isOllamaRunning()) {
            System.out.println("⚠️ Ollama не запущен, пропускаем тест");
            return;
        }

        String question = "What is a vector database?";
        String answer = ragService.ask(question);

        assertThat(answer)
                .isNotNull()
                .isNotEmpty();
        System.out.println("✅ Интеграционный тест пройден");
        System.out.println("📝 Вопрос: " + question);
        System.out.println("📝 Ответ: " + answer);
    }
}