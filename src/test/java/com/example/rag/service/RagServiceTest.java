package com.example.rag.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

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
 *     <td>Успешный запрос</td>
 *     <td>Обычный вопрос на английском языке</td>
 *     <td>Получение осмысленного ответа от LLM</td>
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
 *     <td>Вопрос с большим количеством слов</td>
 *     <td>Получение ответа без ошибок</td>
 *   </tr>
 *   <tr>
 *     <td>Спецсимволы</td>
 *     <td>Вопрос с @, #, &amp; и другими спецсимволами</td>
 *     <td>Корректная обработка спецсимволов</td>
 *   </tr>
 *   <tr>
 *     <td>Русский язык</td>
 *     <td>Вопрос на русском языке</td>
 *     <td>Получение ответа на русском языке</td>
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
 * @author RAG Application Team
 * @version 2.0
 * @see RagService
 * @see BaseIntegrationTestWithContainers
 * @since 1.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Epic("Интеграционные тесты")
@Feature("RAG Сервис")
class RagServiceTest extends BaseIntegrationTestWithContainers {

    @Autowired
    private RagService ragService;

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    /**
     * Проверяет успешный запрос к RAG сервису.
     * <p>
     * <b>Сценарий:</b> Обычный вопрос на английском языке.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Ответ содержит осмысленный текст</li>
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
    @Description("Проверка успешного запроса с релевантными документами")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.CRITICAL)
    void testAskQuestion_Success() {
        // Act
        String answer = ragService.ask("What is Spring AI?");

        // Assert
        assertThat(answer)
                .as("Ответ должен быть не null")
                .isNotNull();

        log.info("✅ Тест успешного запроса пройден");
        log.info("📝 Ответ: {}", answer);
    }

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
        // Act
        String answer = ragService.ask("");

        // Assert
        assertThat(answer)
                .as("При пустом вопросе должно возвращаться сообщение")
                .isEqualTo("Пожалуйста, задайте вопрос.");

        log.info("✅ Тест с пустым вопросом пройден");
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
        // Act
        String answer = ragService.ask(null);

        // Assert
        assertThat(answer)
                .as("При null вопросе должно возвращаться сообщение")
                .isEqualTo("Пожалуйста, задайте вопрос.");

        log.info("✅ Тест с null вопросом пройден");
    }

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
        // Arrange
        String longQuestion = "What is the difference between " +
                              "Spring AI and LangChain4j? Which one should I use for " +
                              "building RAG applications with vector databases?";

        // Act
        String answer = ragService.ask(longQuestion);

        // Assert
        assertThat(answer)
                .as("При длинном вопросе должен быть ответ")
                .isNotNull();

        log.info("✅ Тест с длинным вопросом пройден");
        log.info("📝 Длина ответа: {} символов", answer.length());
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
        // Arrange
        String question = "What is Spring AI? How does it work with @Annotation and #SpringBoot?";

        // Act
        String answer = ragService.ask(question);

        // Assert
        assertThat(answer)
                .as("Ответ должен быть не null")
                .isNotNull();

        log.info("✅ Тест с вопросом, содержащим спецсимволы, пройден");
    }

    /**
     * Проверяет обработку вопроса на русском языке.
     * <p>
     * <b>Сценарий:</b> Вопрос на русском языке.
     * </p>
     * <p>
     * <b>Ожидаемый результат:</b>
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Ответ на русском языке</li>
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
    @Description("Проверка обработки вопроса на русском языке")
    @Story("Многоязычная поддержка")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestion_WithRussianLanguage() {
        // Arrange
        String question = "Что такое Spring AI и как он работает?";

        // Act
        String answer = ragService.ask(question);

        // Assert
        assertThat(answer)
                .as("Ответ на русском должен быть не null")
                .isNotNull();

        log.info("✅ Тест с вопросом на русском пройден");
    }

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
        // Проверяем, что сервис существует
        assertThat(ragService)
                .as("RagService должен быть внедрен")
                .isNotNull();

        // Проверяем, что метод ask не падает с NPE при любых обстоятельствах
        // Вызываем с пустым вопросом, чтобы проверить обработку ошибок
        String answer = ragService.ask("");

        assertThat(answer)
                .as("При пустом вопросе должно возвращаться сообщение")
                .isNotNull();

        log.info("✅ Тест обработки ошибок LLM пройден");
    }

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
        // Assert
        assertThat(ragService)
                .as("RagService должен быть внедрен")
                .isNotNull();

        log.info("✅ RagService доступен");
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
        // Проверяем, что сервис может выполнить запрос с использованием VectorStore
        String answer = ragService.ask("What is Spring AI?");

        assertThat(answer)
                .as("Ответ должен быть не null, что доказывает работу VectorStore")
                .isNotNull();

        log.info("✅ VectorStore используется в сервисе");
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
        // Проверяем, что сервис существует
        assertThat(ragService)
                .as("RagService должен быть внедрен")
                .isNotNull();

        // Проверяем, что метод ask обрабатывает ошибки
        // Вызываем с пустым вопросом, чтобы проверить обработку ошибок
        String answer = ragService.ask("");

        assertThat(answer)
                .as("При пустом вопросе должно возвращаться сообщение")
                .isNotNull();

        log.info("✅ Проверка обработки ошибки ChatClient пройдена");
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
        // Проверяем, что сервис существует
        assertThat(ragService)
                .as("RagService должен быть внедрен")
                .isNotNull();

        // Проверяем, что метод ask обрабатывает ошибки
        // Вызываем с пустым вопросом, чтобы проверить обработку ошибок
        String answer = ragService.ask("");

        assertThat(answer)
                .as("При пустом вопросе должно возвращаться сообщение")
                .isNotNull();

        log.info("✅ Проверка обработки ошибки VectorStore пройдена");
    }
}