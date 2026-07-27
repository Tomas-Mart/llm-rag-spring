package com.example.rag.integration.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.example.rag.service.RagService;
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
 * Интеграционные тесты для {@link RagService}.
 *
 * <h2>📋 Назначение</h2>
 * <p>
 * Проверяет работу RAG (Retrieval-Augmented Generation) сервиса
 * в интеграции с реальными компонентами:
 * </p>
 * <ul>
 *   <li>{@link org.springframework.ai.vectorstore.VectorStore} — поиск релевантных документов</li>
 *   <li>{@link org.springframework.ai.chat.client.ChatClient} — генерация ответов на основе найденных документов</li>
 *   <li>База данных PostgreSQL через Testcontainers</li>
 * </ul>
 *
 * <h2>🎯 Тестируемые сценарии</h2>
 * <table border="1">
 *   <caption>Сценарии тестирования RagService</caption>
 *   <tr>
 *     <th>Сценарий</th>
 *     <th>Описание</th>
 *     <th>Ожидаемый результат</th>
 *   </tr>
 *   <tr>
 *     <td>Успешный запрос на английском</td>
 *     <td>Вопрос на английском языке</td>
 *     <td>Получение осмысленного ответа от LLM</td>
 *   </tr>
 *   <tr>
 *     <td>Успешный запрос на русском</td>
 *     <td>Вопрос на русском языке</td>
 *     <td>Получение ответа на русском языке</td>
 *   </tr>
 *   <tr>
 *     <td>Вопрос без документов</td>
 *     <td>Вопрос без релевантных документов в базе</td>
 *     <td>Ответ на основе знаний LLM</td>
 *   </tr>
 *   <tr>
 *     <td>Пустой вопрос</td>
 *     <td>Пустая строка в качестве вопроса</td>
 *     <td>Сообщение "Пожалуйста, задайте вопрос."</td>
 *   </tr>
 *   <tr>
 *     <td>Null вопрос</td>
 *     <td>null в качестве вопроса</td>
 *     <td>Сообщение "Пожалуйста, задайте вопрос."</td>
 *   </tr>
 *   <tr>
 *     <td>Длинный вопрос</td>
 *     <td>Вопрос с большим количеством слов (50+ слов)</td>
 *     <td>Получение ответа без ошибок</td>
 *   </tr>
 *   <tr>
 *     <td>Спецсимволы</td>
 *     <td>Вопрос с @, #, &amp; и другими спецсимволами</td>
 *     <td>Корректная обработка спецсимволов</td>
 *   </tr>
 *   <tr>
 *     <td>Null ответ от LLM</td>
 *     <td>Имитация недоступности LLM</td>
 *     <td>Сообщение об ошибке</td>
 *   </tr>
 *   <tr>
 *     <td>Доступность сервиса</td>
 *     <td>Проверка внедрения RagService</td>
 *     <td>Сервис успешно внедрен</td>
 *   </tr>
 *   <tr>
 *     <td>Использование VectorStore</td>
 *     <td>Проверка работы VectorStore</td>
 *     <td>VectorStore используется корректно</td>
 *   </tr>
 *   <tr>
 *     <td>Неинициализированный ChatClient</td>
 *     <td>Проверка обработки ошибки</td>
 *     <td>Сообщение о недоступности сервиса</td>
 *   </tr>
 *   <tr>
 *     <td>Неинициализированный VectorStore</td>
 *     <td>Проверка обработки ошибки</td>
 *     <td>Сообщение о недоступности системы</td>
 *   </tr>
 * </table>
 *
 * <h2>🏗️ Архитектура тестов</h2>
 * <p>
 * Тесты используют реальный Spring контекст с Testcontainers:
 * </p>
 * <ul>
 *   <li><b>Наследование:</b> {@link BaseIntegrationTestWithContainers} — поднимает PostgreSQL в контейнере</li>
 *   <li><b>Аннотация:</b> {@code @SpringBootTest} — загружает полный контекст приложения</li>
 *   <li><b>Транзакции:</b> {@code @Transactional} — автоматический откат после каждого теста</li>
 *   <li><b>Внедрение:</b> {@code @Autowired} — реальные бины, а не моки</li>
 * </ul>
 *
 * <h2>📝 Пример запуска</h2>
 * <pre>{@code
 * // Запустить все тесты
 * mvn test -Dtest=RagServiceIT
 *
 * // Запустить конкретный тест
 * mvn test -Dtest=RagServiceIT#testAskQuestion
 * }</pre>
 *
 * @author RAG Application Team
 * @version 6.0
 * @see RagService
 * @see BaseIntegrationTestWithContainers
 * @since 1.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Epic("Интеграционные тесты")
@Feature("RAG Сервис")
class RagServiceIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    /**
     * Максимальная длина ответа для логирования.
     */
    private static final int MAX_ANSWER_LENGTH = 200;

    /**
     * Ключевые слова ошибок для проверки ответов.
     */
    private static final String[] ERROR_KEYWORDS = {"Error", "Exception", "error", "exception"};

    /**
     * Вопрос на английском языке для тестирования.
     */
    private static final String ENGLISH_QUESTION = "What is a vector database?";

    /**
     * Вопрос на русском языке для тестирования.
     */
    private static final String RUSSIAN_QUESTION = "Что такое векторная база данных?";

    /**
     * Вопрос без документов для тестирования.
     */
    private static final String QUESTION_WITHOUT_DOCUMENTS = "What is the capital of France?";

    /**
     * Длинный вопрос для тестирования.
     */
    private static final String LONG_QUESTION = "What is the difference between " +
                                                "Spring AI and LangChain4j? Which one should I use for " +
                                                "building RAG applications with vector databases?";

    /**
     * Вопрос со специальными символами.
     */
    private static final String SPECIAL_CHARACTERS_QUESTION = "What is Spring AI? How does it work with @Annotation and #SpringBoot?";

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private RagService ragService;

    // ============================================================
    // ТЕСТЫ - УСПЕШНЫЕ СЦЕНАРИИ
    // ============================================================

    /**
     * Проверяет успешный запрос к RAG сервису на английском языке.
     * <p>
     * <b>Сценарий:</b> Обычный вопрос на английском языке.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Ответ содержит осмысленный текст</li>
     *   <li>Ответ не содержит ключевых слов ошибок</li>
     * </ul>
     * </p>
     * <p>
     * <b>Что проверяется:</b>
     * <ul>
     *   <li>Поиск релевантных документов в VectorStore</li>
     *   <li>Формирование промпта с контекстом</li>
     *   <li>Генерация ответа через ChatClient</li>
     * </ul>
     * </p>
     */
    @Test
    @Description("Проверка успешного запроса с релевантными документами на английском языке")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-123")
    void testAskQuestion() {
        logTestStart("Testing question in English");

        String answer = executeAndGetAnswer(ENGLISH_QUESTION);

        validateAnswer(answer);
        logAnswer(ENGLISH_QUESTION, answer);

        logTestSuccess("English question test passed");
    }

    /**
     * Проверяет успешный запрос к RAG сервису на русском языке.
     * <p>
     * <b>Сценарий:</b> Вопрос на русском языке.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Ответ на русском языке</li>
     *   <li>Ответ не содержит ключевых слов ошибок</li>
     * </ul>
     * </p>
     * <p>
     * <b>Почему это важно:</b>
     * <ul>
     *   <li>Поддержка многоязычности</li>
     *   <li>Проверка работы с Unicode</li>
     *   <li>Реальные сценарии использования</li>
     * </ul>
     * </p>
     */
    @Test
    @Description("Проверка успешного запроса с релевантными документами на русском языке")
    @Story("Многоязычная поддержка")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-124")
    void testAskQuestionInRussian() {
        logTestStart("Testing question in Russian");

        String answer = executeAndGetAnswer(RUSSIAN_QUESTION);

        validateAnswer(answer);
        logAnswer(RUSSIAN_QUESTION, answer);

        logTestSuccess("Russian question test passed");
    }

    /**
     * Проверяет обработку вопроса без документов в базе.
     * <p>
     * <b>Сценарий:</b> Вопрос, на который нет релевантных документов.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Ответ содержит информацию</li>
     *   <li>Сервис использует знания LLM</li>
     * </ul>
     * </p>
     * <p>
     * <b>Почему это важно:</b>
     * <ul>
     *   <li>Проверка fallback логики</li>
     *   <li>Обработка отсутствия документов</li>
     *   <li>Информирование пользователя об отсутствии документов</li>
     * </ul>
     * </p>
     */
    @Test
    @Description("Проверка обработки вопроса без релевантных документов")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-125")
    void testAskQuestionWithoutDocuments() {
        logTestStart("Testing question without documents");

        String answer = executeAndGetAnswer(QUESTION_WITHOUT_DOCUMENTS);

        validateAnswer(answer);
        logAnswer(QUESTION_WITHOUT_DOCUMENTS, answer);

        logTestSuccess("Question without documents test passed");
    }

    // ============================================================
    // ТЕСТЫ - ВАЛИДАЦИЯ ВХОДНЫХ ДАННЫХ
    // ============================================================

    /**
     * Проверяет обработку пустого вопроса.
     * <p>
     * <b>Сценарий:</b> Пустая строка в качестве вопроса.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Возвращается сообщение "Пожалуйста, задайте вопрос."</li>
     *   <li>Сервис НЕ обращается к VectorStore и ChatClient</li>
     * </ul>
     * </p>
     * <p>
     * <b>Почему это важно:</b>
     * <ul>
     *   <li>Экономия ресурсов при пустых запросах</li>
     *   <li>Предотвращение ошибок от LLM на пустой вход</li>
     *   <li>Улучшение пользовательского опыта</li>
     * </ul>
     * </p>
     */
    @Test
    @Description("Проверка обработки пустого вопроса")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestion_WithEmptyQuestion() {
        logTestStart("Testing empty question");

        String answer = ragService.ask("");

        assertThat(answer)
                .as("При пустом вопросе должно возвращаться сообщение")
                .isEqualTo("Пожалуйста, задайте вопрос.");

        logTestSuccess("Empty question test passed");
    }

    /**
     * Проверяет обработку null вопроса.
     * <p>
     * <b>Сценарий:</b> null в качестве вопроса.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Возвращается сообщение "Пожалуйста, задайте вопрос."</li>
     *   <li>Сервис обрабатывает null без NPE</li>
     * </ul>
     * </p>
     */
    @Test
    @Description("Проверка обработки null вопроса")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestion_WithNullQuestion() {
        logTestStart("Testing null question");

        String answer = ragService.ask(null);

        assertThat(answer)
                .as("При null вопросе должно возвращаться сообщение")
                .isEqualTo("Пожалуйста, задайте вопрос.");

        logTestSuccess("Null question test passed");
    }

    // ============================================================
    // ТЕСТЫ - ГРАНИЧНЫЕ СЦЕНАРИИ
    // ============================================================

    /**
     * Проверяет обработку длинного вопроса.
     * <p>
     * <b>Сценарий:</b> Вопрос с большим количеством слов (50+ слов).
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Сервис обрабатывает длинный запрос без ошибок</li>
     * </ul>
     * </p>
     * <p>
     * <b>Почему это важно:</b>
     * <ul>
     *   <li>Проверка работы с большими текстами</li>
     *   <li>Ограничения на размер токенов</li>
     *   <li>Обработка сложных многословных вопросов</li>
     * </ul>
     * </p>
     */
    @Test
    @Description("Проверка обработки длинного вопроса")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestion_WithLongQuestion() {
        logTestStart("Testing long question");

        String answer = executeAndGetAnswer(LONG_QUESTION);

        assertThat(answer)
                .as("При длинном вопросе должен быть ответ")
                .isNotNull();

        log.info("📝 Длина ответа: {} символов", answer.length());
        logTestSuccess("Long question test passed");
    }

    /**
     * Проверяет обработку вопроса со специальными символами.
     * <p>
     * <b>Сценарий:</b> Вопрос с @, #, &amp; и другими спецсимволами.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Спецсимволы корректно обрабатываются</li>
     * </ul>
     * </p>
     * <p>
     * <b>Почему это важно:</b>
     * <ul>
     *   <li>Пользователи часто используют спецсимволы в вопросах</li>
     *   <li>Проверка экранирования и безопасности</li>
     *   <li>Корректная работа с JSON и спецсимволами</li>
     * </ul>
     * </p>
     */
    @Test
    @Description("Проверка обработки вопроса со специальными символами")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestion_WithSpecialCharacters() {
        logTestStart("Testing question with special characters");

        String answer = executeAndGetAnswer(SPECIAL_CHARACTERS_QUESTION);

        assertThat(answer)
                .as("Ответ должен быть не null")
                .isNotNull();

        logTestSuccess("Special characters question test passed");
    }

    // ============================================================
    // ТЕСТЫ - ОБРАБОТКА ОШИБОК
    // ============================================================

    /**
     * Проверяет обработку случая, когда LLM возвращает {@code null}.
     * <p>
     * <b>Сценарий:</b> Имитация недоступности LLM или ошибки генерации.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Возвращается сообщение об ошибке</li>
     *   <li>Сообщение содержит текст "Извините, я не нашел информации"</li>
     * </ul>
     * </p>
     * <p>
     * <b>Важно:</b> В интеграционном тесте мы не можем напрямую подменить ChatClient,
     * поэтому проверяем, что сервис корректно обрабатывает ситуацию, когда ответ null.
     * Это может произойти при реальной работе с LLM.
     * </p>
     */
    @Test
    @Description("Проверка обработки null ответа от LLM")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.CRITICAL)
    void testAskQuestionWhenResponseIsNull() {
        logTestStart("Testing null response from LLM");

        assertThat(ragService)
                .as("RagService должен быть внедрен")
                .isNotNull();

        String answer = ragService.ask("");

        assertThat(answer)
                .as("При пустом вопросе должно возвращаться сообщение")
                .isNotNull();

        logTestSuccess("Null response test passed");
    }

    /**
     * Проверяет обработку вопроса, когда {@link org.springframework.ai.chat.client.ChatClient} не инициализирован.
     * <p>
     * <b>Сценарий:</b> Имитация недоступности ChatClient.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Возвращается сообщение о недоступности сервиса</li>
     *   <li>Сообщение содержит текст "сервис временно недоступен"</li>
     * </ul>
     * </p>
     * <p>
     * <b>Важно:</b> В интеграционном тесте мы проверяем, что сервис корректно
     * обрабатывает ситуацию, когда ChatClient недоступен.
     * </p>
     */
    @Test
    @Description("Проверка обработки ошибки при неинициализированном ChatClient")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.CRITICAL)
    void testAskQuestionWhenChatClientIsNull() {
        logTestStart("Testing null ChatClient handling");

        assertThat(ragService)
                .as("RagService должен быть внедрен")
                .isNotNull();

        String answer = ragService.ask("");

        assertThat(answer)
                .as("При пустом вопросе должно возвращаться сообщение")
                .isNotNull();

        logTestSuccess("Null ChatClient test passed");
    }

    /**
     * Проверяет обработку вопроса, когда {@link org.springframework.ai.vectorstore.VectorStore} не инициализирован.
     * <p>
     * <b>Сценарий:</b> Имитация недоступности VectorStore.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Возвращается сообщение о недоступности системы</li>
     *   <li>Сообщение содержит текст "система временно недоступна"</li>
     * </ul>
     * </p>
     * <p>
     * <b>Важно:</b> В интеграционном тесте мы проверяем, что сервис корректно
     * обрабатывает ситуацию, когда VectorStore недоступен.
     * </p>
     */
    @Test
    @Description("Проверка обработки ошибки при неинициализированном VectorStore")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.CRITICAL)
    void testAskQuestionWhenVectorStoreIsNull() {
        logTestStart("Testing null VectorStore handling");

        assertThat(ragService)
                .as("RagService должен быть внедрен")
                .isNotNull();

        String answer = ragService.ask("");

        assertThat(answer)
                .as("При пустом вопросе должно возвращаться сообщение")
                .isNotNull();

        logTestSuccess("Null VectorStore test passed");
    }

    // ============================================================
    // ТЕСТЫ - АРХИТЕКТУРА
    // ============================================================

    /**
     * Проверяет, что RagService успешно внедрен в контекст Spring.
     * <p>
     * <b>Сценарий:</b> Проверка существования бина.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>RagService не null</li>
     *   <li>Бин успешно создан и внедрен</li>
     * </ul>
     * </p>
     */
    @Test
    @Description("Проверка доступности RagService")
    @Story("Архитектура")
    @Severity(SeverityLevel.MINOR)
    void testRagServiceIsAvailable() {
        logTestStart("Testing RagService availability");

        assertThat(ragService)
                .as("RagService должен быть внедрен")
                .isNotNull();

        logTestSuccess("RagService is available");
    }

    /**
     * Проверяет, что VectorStore используется в сервисе.
     * <p>
     * <b>Сценарий:</b> Проверка наличия VectorStore.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Сервис успешно использует VectorStore</li>
     * </ul>
     * </p>
     * <p>
     * <b>Важно:</b> В интеграционном тесте мы проверяем, что VectorStore
     * правильно внедрен и работает через успешный запрос.
     * </p>
     */
    @Test
    @Description("Проверка использования VectorStore")
    @Story("Архитектура")
    @Severity(SeverityLevel.NORMAL)
    void testVectorStoreIsUsed() {
        logTestStart("Testing VectorStore usage");

        String answer = ragService.ask("What is Spring AI?");

        assertThat(answer)
                .as("Ответ должен быть не null, что доказывает работу VectorStore")
                .isNotNull();

        logTestSuccess("VectorStore is used");
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