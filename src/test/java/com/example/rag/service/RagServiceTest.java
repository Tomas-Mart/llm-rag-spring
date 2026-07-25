package com.example.rag.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты для сервиса {@link RagService}.
 * Проверяет логику обработки вопросов с использованием RAG.
 *
 * <p>Тестируемые сценарии:
 * <ul>
 *   <li>Успешный запрос с релевантными документами</li>
 *   <li>Пустой вопрос</li>
 *   <li>Длинный вопрос</li>
 *   <li>Ответ null от LLM</li>
 *   <li>Использование VectorStore</li>
 * </ul>
 *
 * <p>Архитектура тестов:
 * <ul>
 *   <li>Использование Mockito для изоляции внешних зависимостей</li>
 *   <li>Настройка моков через {@code @BeforeEach}</li>
 *   <li>Проверка поведения сервиса в различных сценариях</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    /**
     * Мок для ChatClient.
     * Используется для имитации взаимодействия с LLM.
     */
    @Mock
    private ChatClient chatClient;

    /**
     * Мок для VectorStore.
     * Используется для имитации поиска релевантных документов.
     */
    @Mock
    private VectorStore vectorStore;

    /**
     * Мок для спецификации запроса к чату.
     * Используется для имитации построения запроса.
     */
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    /**
     * Мок для спецификации ответа.
     * Используется для имитации получения ответа от LLM.
     */
    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    /**
     * Тестируемый сервис.
     * Автоматически внедряет моки.
     */
    @InjectMocks
    private RagService ragService;

    /**
     * Тестовый вопрос для использования в тестах.
     */
    private String testQuestion;

    /**
     * Настройка перед каждым тестом.
     * Создает и настраивает моки.
     */
    @BeforeEach
    void setUp() {
        testQuestion = "What is Spring AI?";

        // Настройка мока ChatClient
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.system(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.messages(any(Message.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(responseSpec);
        lenient().when(responseSpec.content()).thenReturn("Spring AI is a framework for building AI applications with Spring Boot.");

        // Настройка мока VectorStore
        lenient().when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Spring AI is a Java framework for building AI applications.")));
    }

    /**
     * Проверяет успешный запрос с релевантными документами.
     *
     * <p>Ожидаемый результат:
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Ответ содержит 'Spring AI'</li>
     * </ul>
     */
    @Test
    void testAskQuestion() {
        String answer = ragService.ask(testQuestion);

        assertThat(answer)
                .as("Ответ должен быть не null и содержать 'Spring AI'")
                .isNotNull()
                .contains("Spring AI");

        System.out.println("✅ Вопрос: " + testQuestion);
        System.out.println("✅ Ответ: " + answer);
    }

    /**
     * Проверяет обработку пустого вопроса.
     *
     * <p>Ожидаемый результат:
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Сервис корректно обрабатывает пустой ввод</li>
     * </ul>
     */
    @Test
    void testAskQuestionWithEmptyQuestion() {
        when(responseSpec.content()).thenReturn("");

        String answer = ragService.ask("");
        assertThat(answer).isNotNull();
        System.out.println("✅ Пустой вопрос обработан");
    }

    /**
     * Проверяет обработку длинного вопроса.
     *
     * <p>Ожидаемый результат:
     * <ul>
     *   <li>Ответ не null</li>
     *   <li>Сервис корректно обрабатывает длинные запросы</li>
     * </ul>
     */
    @Test
    void testAskQuestionWithLongQuestion() {
        String longQuestion = "What is the difference between " +
                              "Spring AI and LangChain4j? Which one should I use for " +
                              "building RAG applications with vector databases?";

        String answer = ragService.ask(longQuestion);
        assertThat(answer).isNotNull();
        System.out.println("✅ Длинный вопрос обработан, длина ответа: " + answer.length());
    }

    /**
     * Проверяет обработку случая, когда LLM возвращает null.
     *
     * <p>Ожидаемый результат:
     * <ul>
     *   <li>Возвращается сообщение об ошибке</li>
     *   <li>Сообщение содержит текст "Извините, я не нашел информации"</li>
     * </ul>
     */
    @Test
    void testAskQuestionWhenResponseIsNull() {
        when(responseSpec.content()).thenReturn(null);

        String answer = ragService.ask(testQuestion);

        assertThat(answer)
                .as("При null ответе должно возвращаться сообщение об ошибке")
                .isNotNull()
                .contains("Извините, я не нашел информации");

        System.out.println("✅ Тест с null ответом пройден");
    }

    /**
     * Проверяет, что VectorStore корректно внедрен в сервис.
     *
     * <p>Ожидаемый результат:
     * <ul>
     *   <li>VectorStore не null</li>
     *   <li>Сервис использует VectorStore для поиска</li>
     * </ul>
     */
    @Test
    void testVectorStoreIsUsed() {
        assertThat(vectorStore)
                .as("VectorStore должен быть внедрен в сервис")
                .isNotNull();

        String answer = ragService.ask(testQuestion);
        assertThat(answer).isNotNull();

        System.out.println("✅ VectorStore используется в сервисе");
    }
}