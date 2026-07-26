package com.example.rag.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.rag.service.RagService;
import com.example.rag.support.BaseIntegrationTest;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Интеграционный тест для проверки работы {@link RagService}.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет работу {@link RagService} с реальной LLM через Ollama
 * и реальной базой данных PostgreSQL.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Задание вопроса на английском языке</li>
 *   <li>Задание вопроса на русском языке</li>
 *   <li>Задание вопроса без документов в базе</li>
 * </ul>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Использует реальный {@link RagService}</li>
 *   <li>Проверяет корректность ответов</li>
 *   <li>Все аннотации наследуются от {@link BaseIntegrationTest}</li>
 * </ul>
 *
 * <h2>Пример запуска</h2>
 * <pre>{@code
 * // Запустить все тесты
 * mvn test -Dtest=RagServiceIT
 *
 * // Запустить конкретный тест
 * mvn test -Dtest=RagServiceIT#testAskQuestion
 * }</pre>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see RagService
 * @see BaseIntegrationTest
 * @since 1.0
 */
@Slf4j
@Epic("Интеграционные тесты")
@Feature("RAG Сервис")
class RagServiceIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final int MAX_ANSWER_LENGTH = 200;
    private static final String[] ERROR_KEYWORDS = {"Error", "Exception", "error", "exception"};

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private RagService ragService;

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Тест проверяет возможность задать вопрос на английском языке")
    @Story("Работа с вопросами")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-123")
    void testAskQuestion() {
        logTestStart("Testing question in English");

        String question = "What is a vector database?";
        String answer = executeAndGetAnswer(question);

        validateAnswer(answer);
        logAnswer(question, answer);

        logTestSuccess("English question test passed");
    }

    @Test
    @Description("Тест проверяет возможность задать вопрос на русском языке")
    @Story("Работа с вопросами на русском языке")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-124")
    void testAskQuestionInRussian() {
        logTestStart("Testing question in Russian");

        String question = "Что такое векторная база данных?";
        String answer = executeAndGetAnswer(question);

        validateAnswer(answer);
        logAnswer(question, answer);

        logTestSuccess("Russian question test passed");
    }

    @Test
    @Description("Тест проверяет обработку вопроса без документов")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-125")
    void testAskQuestionWithoutDocuments() {
        logTestStart("Testing question without documents");

        String question = "What is the capital of France?";
        String answer = executeAndGetAnswer(question);

        validateAnswer(answer);
        logAnswer(question, answer);

        logTestSuccess("Question without documents test passed");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Выполняет запрос к RagService и возвращает ответ.
     *
     * @param question вопрос пользователя
     * @return ответ от сервиса
     */
    private String executeAndGetAnswer(String question) {
        assertThatCode(() -> ragService.ask(question))
                .as("Question should not throw exception")
                .doesNotThrowAnyException();

        return ragService.ask(question);
    }

    /**
     * Проверяет корректность ответа.
     *
     * @param answer ответ для проверки
     */
    private void validateAnswer(String answer) {
        assertThat(answer)
                .as("Answer should not be null or empty")
                .isNotNull()
                .isNotEmpty();

        for (String keyword : ERROR_KEYWORDS) {
            assertThat(answer)
                    .as("Answer should not contain '%s'", keyword)
                    .doesNotContain(keyword);
        }

        log.info("✅ Answer validation passed");
    }

    /**
     * Логирует вопрос и ответ.
     *
     * @param question вопрос пользователя
     * @param answer   ответ от сервиса
     */
    private void logAnswer(String question, String answer) {
        log.info("📝 Question: {}", question);
        log.info("📝 Answer: {}...", truncateAnswer(answer));
    }

    /**
     * Обрезает ответ до максимальной длины.
     *
     * @param answer ответ для обрезки
     * @return обрезанный ответ
     */
    private String truncateAnswer(String answer) {
        if (answer == null) {
            return "null";
        }
        if (answer.length() <= MAX_ANSWER_LENGTH) {
            return answer;
        }
        return answer.substring(0, MAX_ANSWER_LENGTH) + "...";
    }
}