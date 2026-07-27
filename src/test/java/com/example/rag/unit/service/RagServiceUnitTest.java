package com.example.rag.unit.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import com.example.rag.service.RagService;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Модульные тесты для {@link RagService}.
 *
 * <p>Тестирует логику RAG (Retrieval-Augmented Generation) в изоляции
 * от реальных зависимостей (ChatClient, VectorStore).</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Успешный ответ с документами</li>
 *   <li>Ответ без документов</li>
 *   <li>Пустой вопрос</li>
 *   <li>Ошибки ChatClient и VectorStore</li>
 *   <li>Пустой ответ от LLM</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see RagService
 * @since 1.0
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Модульные тесты RagService")
class RagServiceUnitTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private RagService ragService;

    private static final String TEST_QUESTION = "Что такое Spring AI?";
    private static final String TEST_DOCUMENT_CONTENT = "Spring AI is a framework for building AI applications.";
    private static final String EXPECTED_RESPONSE = "Spring AI - это фреймворк для создания AI-приложений.";

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ МОКОВ
    // ============================================================

    /**
     * Настраивает моки для успешного ответа от ChatClient.
     *
     * @param response ожидаемый ответ
     */
    private void setupChatClientMock(String response) {
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.messages(any(Message[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(response);
    }

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        // Базовая настройка - не используется напрямую,
        // каждый тест настраивает свои моки
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ ask() - УСПЕШНЫЕ СЦЕНАРИИ
    // ============================================================

    @Test
    @DisplayName("Успешный ответ с документами")
    void testAsk_SuccessWithDocuments() {
        // Given
        Document document = new Document(TEST_DOCUMENT_CONTENT);
        List<Document> documents = List.of(document);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
        setupChatClientMock(EXPECTED_RESPONSE);

        // When
        String result = ragService.ask(TEST_QUESTION);

        // Then
        assertThat(result).isEqualTo(EXPECTED_RESPONSE);
        log.info("✅ Тест успешного ответа с документами пройден");
    }

    @Test
    @DisplayName("Успешный ответ с несколькими документами")
    void testAsk_SuccessWithMultipleDocuments() {
        // Given
        Document doc1 = new Document("First document content.");
        Document doc2 = new Document("Second document content.");
        Document doc3 = new Document("Third document content.");
        List<Document> documents = List.of(doc1, doc2, doc3);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
        setupChatClientMock(EXPECTED_RESPONSE);

        // When
        String result = ragService.ask(TEST_QUESTION);

        // Then
        assertThat(result).isEqualTo(EXPECTED_RESPONSE);
        log.info("✅ Тест с несколькими документами пройден");
    }

    @Test
    @DisplayName("Ответ без документов (только знания LLM)")
    void testAsk_WithoutDocuments() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        setupChatClientMock(EXPECTED_RESPONSE);

        // When
        String result = ragService.ask(TEST_QUESTION);

        // Then
        assertThat(result).isEqualTo(EXPECTED_RESPONSE);
        log.info("✅ Тест ответа без документов пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ ask() - ВАЛИДАЦИЯ ВХОДНЫХ ДАННЫХ
    // ============================================================

    @Test
    @DisplayName("Пустой вопрос - null")
    void testAsk_WithNullQuestion() {
        // When
        String result = ragService.ask(null);

        // Then
        assertThat(result).isEqualTo("Пожалуйста, задайте вопрос.");
        log.info("✅ Тест с null вопросом пройден");
    }

    @Test
    @DisplayName("Пустой вопрос - пустая строка")
    void testAsk_WithEmptyQuestion() {
        // When
        String result = ragService.ask("");

        // Then
        assertThat(result).isEqualTo("Пожалуйста, задайте вопрос.");
        log.info("✅ Тест с пустым вопросом пройден");
    }

    @Test
    @DisplayName("Пустой вопрос - пробелы")
    void testAsk_WithWhitespaceQuestion() {
        // When
        String result = ragService.ask("   ");

        // Then
        assertThat(result).isEqualTo("Пожалуйста, задайте вопрос.");
        log.info("✅ Тест с вопросом из пробелов пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ ask() - ПУСТОЙ ОТВЕТ ОТ LLM
    // ============================================================

    @Test
    @DisplayName("Пустой ответ от LLM")
    void testAsk_WithEmptyResponse() {
        // Given
        Document document = new Document(TEST_DOCUMENT_CONTENT);
        List<Document> documents = List.of(document);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
        setupChatClientMock("");

        // When
        String result = ragService.ask(TEST_QUESTION);

        // Then
        assertThat(result)
                .contains("Извините, я не нашел информации")
                .contains("уточните вопрос или загрузите документ");
        log.info("✅ Тест с пустым ответом LLM пройден");
    }

    @Test
    @DisplayName("Null ответ от LLM")
    void testAsk_WithNullResponse() {
        // Given
        Document document = new Document(TEST_DOCUMENT_CONTENT);
        List<Document> documents = List.of(document);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
        setupChatClientMock(null);

        // When
        String result = ragService.ask(TEST_QUESTION);

        // Then
        assertThat(result)
                .contains("Извините, я не нашел информации")
                .contains("уточните вопрос или загрузите документ");
        log.info("✅ Тест с null ответом LLM пройден");
    }

    @Test
    @DisplayName("Ответ только с пробелами от LLM")
    void testAsk_WithWhitespaceResponse() {
        // Given
        Document document = new Document(TEST_DOCUMENT_CONTENT);
        List<Document> documents = List.of(document);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
        setupChatClientMock("   \n\t   ");

        // When
        String result = ragService.ask(TEST_QUESTION);

        // Then
        assertThat(result)
                .contains("Извините, я не нашел информации")
                .contains("уточните вопрос или загрузите документ");
        log.info("✅ Тест с ответом из пробелов пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ ask() - ОШИБКИ
    // ============================================================

    @Test
    @DisplayName("Ошибка при поиске в VectorStore")
    void testAsk_WithVectorStoreException() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("VectorStore connection error"));

        // When
        String result = ragService.ask(TEST_QUESTION);

        // Then
        assertThat(result)
                .contains("Извините, произошла ошибка")
                .contains("попробуйте позже");
        log.info("✅ Тест с ошибкой VectorStore пройден");
    }

    @Test
    @DisplayName("Ошибка при вызове ChatClient")
    void testAsk_WithChatClientException() {
        // Given
        Document document = new Document(TEST_DOCUMENT_CONTENT);
        List<Document> documents = List.of(document);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        // Создаем мок с исключением
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.messages(any(org.springframework.ai.chat.messages.Message[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenThrow(new RuntimeException("ChatClient connection error"));

        // When
        String result = ragService.ask(TEST_QUESTION);

        // Then
        assertThat(result)
                .contains("Извините, произошла ошибка")
                .contains("попробуйте позже");
        log.info("✅ Тест с ошибкой ChatClient пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ ask() - ПРОВЕРКА ВЕТОК КОДА
    // ============================================================

    @Test
    @DisplayName("Ответ с пустым списком документов")
    void testAsk_WithEmptyDocumentList() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        setupChatClientMock(EXPECTED_RESPONSE);

        // When
        String result = ragService.ask(TEST_QUESTION);

        // Then
        assertThat(result).isEqualTo(EXPECTED_RESPONSE);
        log.info("✅ Тест с пустым списком документов пройден");
    }

    @Test
    @DisplayName("Ответ с null ChatClient")
    void testAsk_WithNullChatClient() {
        // Given
        RagService service = new RagService(null, vectorStore);

        // When
        String result = service.ask(TEST_QUESTION);

        // Then
        assertThat(result)
                .contains("Извините, сервис временно недоступен")
                .contains("Пожалуйста, попробуйте позже");
        log.info("✅ Тест с null ChatClient пройден");
    }

    @Test
    @DisplayName("Ответ с null VectorStore")
    void testAsk_WithNullVectorStore() {
        // Given
        RagService service = new RagService(chatClient, null);

        // When
        String result = service.ask(TEST_QUESTION);

        // Then
        assertThat(result)
                .contains("Извините, система временно недоступна")
                .contains("Пожалуйста, попробуйте позже");
        log.info("✅ Тест с null VectorStore пройден");
    }

    // ============================================================
    // ПОЛНЫЙ ЦИКЛ RAG
    // ============================================================

    @Test
    @DisplayName("Полный цикл RAG с документами")
    void testAsk_FullRagCycle() {
        // Given
        String documentContent = """
                Spring AI is a framework for building AI applications with Spring Boot.
                It provides integration with various LLM providers like OpenAI, Ollama, and more.
                """;
        Document document = new Document(documentContent);
        List<Document> documents = List.of(document);

        String expectedFullResponse = "Spring AI - это фреймворк для создания AI-приложений с Spring Boot. " +
                                      "Он предоставляет интеграцию с различными LLM провайдерами.";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
        setupChatClientMock(expectedFullResponse);

        // When
        String result = ragService.ask("Что такое Spring AI?");

        // Then
        assertThat(result).isEqualTo(expectedFullResponse);
        assertThat(result).isNotEmpty();
        log.info("✅ Тест полного цикла RAG пройден");
    }
}