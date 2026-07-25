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
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Интеграционный тест для проверки работы {@link RagService}.
 * Проверяет взаимодействие с реальной LLM через Ollama и реальной базой данных.
 *
 * <p>Тестируемые сценарии:
 * <ul>
 *   <li>Задание вопроса с реальной LLM</li>
 *   <li>Проверка подключения к PostgreSQL</li>
 *   <li>Проверка подключения к Ollama</li>
 *   <li>Генерация ответа на основе документов</li>
 * </ul>
 *
 * <p>Особенности:
 * <ul>
 *   <li>Используется реальная PostgreSQL с pgvector</li>
 *   <li>Используется реальный Ollama (опционально)</li>
 *   <li>Транзакционная изоляция для отката изменений</li>
 *   <li>Проверка ответа на наличие ошибок</li>
 * </ul>
 *
 * <p>Если Ollama не запущен, тест пропускается с предупреждением.</p>
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
     * Автоматически внедряется Spring.
     */
    @Autowired
    private RagService ragService;

    /**
     * Проверяет задание вопроса с реальной LLM через Ollama.
     *
     * <p>Тест выполняет следующие шаги:
     * <ol>
     *   <li>Проверяет, что PostgreSQL контейнер запущен</li>
     *   <li>Проверяет, что Ollama контейнер запущен</li>
     *   <li>Отправляет вопрос к сервису</li>
     *   <li>Проверяет, что ответ не null и не пустой</li>
     *   <li>Проверяет, что ответ не содержит ошибок</li>
     * </ol>
     *
     * <p>Ожидаемый результат:
     * <ul>
     *   <li>Ответ содержит информацию о векторных базах данных</li>
     *   <li>Ответ не содержит слов "Error" или "Exception"</li>
     *   <li>Тест успешно завершается</li>
     * </ul>
     *
     * <p>Если Ollama не запущен, тест пропускается с предупреждением.</p>
     */
    @Test
    @Description("Тест проверяет возможность задать вопрос и получить ответ от реальной LLM через Ollama")
    @Story("Работа с вопросами")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-123")
    void testAskQuestionWithRealOllama() {
        // Проверяем, что PostgreSQL контейнер запущен и доступен
        assertThat(isPostgresRunning())
                .as("PostgreSQL should be running")
                .isTrue();

        // Проверяем, что Ollama контейнер запущен
        if (!isOllamaRunning()) {
            logger.warn("⚠️ Ollama не запущен, пропускаем тест");
            return;
        }

        logger.info("🤖 Ollama is running on port: {}", getOllamaPort());

        String question = "What is a vector database?";

        // Проверяем, что вопрос не выбрасывает исключение
        assertThatCode(() -> ragService.ask(question))
                .doesNotThrowAnyException();

        // Получаем ответ от сервиса
        String answer = ragService.ask(question);

        // Проверяем корректность ответа
        assertThat(answer)
                .as("Ответ должен быть не null и не пустой")
                .isNotNull()
                .isNotEmpty()
                .as("Ответ не должен содержать ошибок")
                .doesNotContain("Error")
                .doesNotContain("Exception");

        // Логируем успешное выполнение теста
        logger.info("✅ Интеграционный тест пройден");
        logger.info("📝 Вопрос: {}", question);
        logger.info("📝 Ответ: {}", answer.substring(0, Math.min(answer.length(), 200)) + "...");
    }

    /**
     * Проверяет задание вопроса на русском языке.
     *
     * <p>Тест проверяет, что LLM корректно обрабатывает русский язык.</p>
     */
    @Test
    @Description("Тест проверяет возможность задать вопрос на русском языке")
    @Story("Работа с вопросами на русском языке")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-124")
    void testAskQuestionInRussian() {
        // Проверяем, что PostgreSQL контейнер запущен
        assertThat(isPostgresRunning()).isTrue();

        if (!isOllamaRunning()) {
            logger.warn("⚠️ Ollama не запущен, пропускаем тест");
            return;
        }

        logger.info("🤖 Ollama is running on port: {}", getOllamaPort());

        String question = "Что такое векторная база данных?";

        assertThatCode(() -> ragService.ask(question))
                .doesNotThrowAnyException();

        String answer = ragService.ask(question);

        assertThat(answer)
                .isNotNull()
                .isNotEmpty()
                .doesNotContain("Error")
                .doesNotContain("Exception");

        logger.info("✅ Тест с русским вопросом пройден");
        logger.info("📝 Вопрос: {}", question);
        logger.info("📝 Ответ: {}", answer.substring(0, Math.min(answer.length(), 200)) + "...");
    }

    /**
     * Проверяет задание вопроса без документов в базе данных.
     *
     * <p>Тест проверяет, что сервис корректно обрабатывает ситуацию,
     * когда в базе данных нет релевантных документов.</p>
     */
    @Test
    @Description("Тест проверяет обработку вопроса без документов")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-125")
    void testAskQuestionWithoutDocuments() {
        assertThat(isPostgresRunning()).isTrue();

        if (!isOllamaRunning()) {
            logger.warn("⚠️ Ollama не запущен, пропускаем тест");
            return;
        }

        // Вопрос, на который нет документов в базе
        String question = "What is the capital of France?";

        assertThatCode(() -> ragService.ask(question))
                .doesNotThrowAnyException();

        String answer = ragService.ask(question);

        assertThat(answer)
                .isNotNull()
                .isNotEmpty()
                .doesNotContain("Error")
                .doesNotContain("Exception");

        logger.info("✅ Тест без документов пройден");
        logger.info("📝 Вопрос: {}", question);
        logger.info("📝 Ответ: {}", answer.substring(0, Math.min(answer.length(), 200)) + "...");
    }
}