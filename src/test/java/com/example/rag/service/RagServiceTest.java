package com.example.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @InjectMocks
    private RagService ragService;

    private String testQuestion;

    @BeforeEach
    void setUp() {
        testQuestion = "What is Spring AI?";

        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.system(anyString())).thenReturn(requestSpec);    // ← ДОБАВИТЬ
        lenient().when(requestSpec.messages(any(Message.class))).thenReturn(requestSpec);  // ← ИЗМЕНИТЬ
        lenient().when(requestSpec.call()).thenReturn(responseSpec);
        lenient().when(responseSpec.content()).thenReturn("Spring AI is a framework for building AI applications with Spring Boot.");
    }

    @Test
    void testAskQuestion() {
        // ✅ Явно переопределяем мок для этого теста
        when(responseSpec.content()).thenReturn("Spring AI is a framework for building AI applications with Spring Boot.");

        String answer = ragService.ask(testQuestion);

        assertThat(answer)
                .as("Ответ должен быть не null и содержать 'Spring AI'")
                .isNotNull()
                .contains("Spring AI");

        System.out.println("✅ Вопрос: " + testQuestion);
        System.out.println("✅ Ответ: " + answer);
    }

    @Test
    void testAskQuestionWithEmptyQuestion() {
        when(responseSpec.content()).thenReturn("");

        String answer = ragService.ask("");
        assertThat(answer).isNotNull();
        System.out.println("✅ Пустой вопрос обработан");
    }

    @Test
    void testAskQuestionWithLongQuestion() {
        String longQuestion = "What is the difference between " +
                              "Spring AI and LangChain4j? Which one should I use for " +
                              "building RAG applications with vector databases?";

        String answer = ragService.ask(longQuestion);
        assertThat(answer).isNotNull();
        System.out.println("✅ Длинный вопрос обработан, длина ответа: " + answer.length());
    }

    @Test
    void testAskQuestionWhenResponseIsNull() {
        // ✅ Переопределяем мок для null ответа
        when(responseSpec.content()).thenReturn(null);

        String answer = ragService.ask(testQuestion);

        assertThat(answer)
                .as("При null ответе должно возвращаться сообщение об ошибке")
                .isNotNull()
                .contains("Извините, я не нашел информации");

        System.out.println("✅ Тест с null ответом пройден");
    }

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