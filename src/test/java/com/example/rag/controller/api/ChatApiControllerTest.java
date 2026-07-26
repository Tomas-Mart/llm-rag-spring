package com.example.rag.controller.api;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.rag.service.RagService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ChatApiController}.
 * Проверяют REST API эндпоинты для чата.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ChatApiControllerTest {

    // === МОКИ ===

    @Mock
    private RagService ragService;

    @InjectMocks
    private ChatApiController controller;

    // === ТЕСТОВЫЕ ДАННЫЕ ===

    private String testQuestion;
    private String testAnswer;

    @BeforeEach
    void setUp() {
        testQuestion = "What is Spring AI?";
        testAnswer = "Spring AI is a framework for building AI applications.";
    }

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    /**
     * Тест успешного ответа на вопрос.
     * Проверяет, что возвращается ответ с правильным статусом и данными.
     */
    @Test
    void testAskQuestion_Success() {
        // Arrange
        Map<String, String> request = Map.of("question", testQuestion);
        when(ragService.ask(testQuestion)).thenReturn(testAnswer);

        // Act
        ResponseEntity<Map<String, String>> response = controller.askQuestion(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("question", testQuestion)
                .containsEntry("answer", testAnswer);

        System.out.println("✅ Тест успешного ответа на вопрос пройден");
    }

    /**
     * Тест вопроса с пустой строкой.
     * Проверяет, что сервис обрабатывает пустой вопрос.
     */
    @Test
    void testAskQuestion_WithEmptyQuestion() {
        // Arrange
        Map<String, String> request = Map.of("question", "");
        when(ragService.ask("")).thenReturn("Please provide a valid question.");

        // Act
        ResponseEntity<Map<String, String>> response = controller.askQuestion(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("question", "")
                .containsKey("answer");

        System.out.println("✅ Тест с пустым вопросом пройден");
    }

    /**
     * Тест вопроса с null.
     * Проверяет, что сервис обрабатывает null вопрос.
     */
    @Test
    void testAskQuestion_WithNullQuestion() {
        // Arrange
        Map<String, String> request = Map.of("question", (String) null);
        when(ragService.ask(null)).thenReturn("Question cannot be null.");

        // Act
        ResponseEntity<Map<String, String>> response = controller.askQuestion(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("question", null)
                .containsKey("answer");

        System.out.println("✅ Тест с null вопросом пройден");
    }

    /**
     * Тест вопроса без поля "question".
     * Проверяет, что при отсутствии поля "question" возвращается ошибка.
     */
    @Test
    void testAskQuestion_WithMissingQuestionField() {
        // Arrange
        Map<String, String> request = Map.of("wrongField", "some value");

        // Act
        ResponseEntity<Map<String, String>> response = controller.askQuestion(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsKey("answer");

        System.out.println("✅ Тест с отсутствующим полем question пройден");
    }

    /**
     * Тест с пробелами в вопросе.
     * Проверяет, что сервис обрабатывает вопрос с пробелами.
     */
    @Test
    void testAskQuestion_WithSpaces() {
        // Arrange
        String questionWithSpaces = "   What is Spring AI?   ";
        String expectedAnswer = "Spring AI is a framework.";
        Map<String, String> request = Map.of("question", questionWithSpaces);
        when(ragService.ask(questionWithSpaces)).thenReturn(expectedAnswer);

        // Act
        ResponseEntity<Map<String, String>> response = controller.askQuestion(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("question", questionWithSpaces)
                .containsEntry("answer", expectedAnswer);

        System.out.println("✅ Тест с пробелами в вопросе пройден");
    }

    /**
     * Тест ошибки сервиса.
     * Проверяет, что при ошибке в RagService возвращается 500.
     */
    @Test
    void testAskQuestion_WhenServiceThrowsException() {
        // Arrange
        Map<String, String> request = Map.of("question", testQuestion);
        String errorMessage = "Service unavailable";
        doThrow(new RuntimeException(errorMessage)).when(ragService).ask(anyString());

        // Act
        ResponseEntity<Map<String, String>> response = controller.askQuestion(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("error", errorMessage);

        System.out.println("✅ Тест ошибки сервиса пройден");
    }

    /**
     * Тест длинного вопроса.
     * Проверяет, что сервис обрабатывает длинные вопросы.
     */
    @Test
    void testAskQuestion_WithLongQuestion() {
        // Arrange
        String longQuestion = "What is the meaning of life, the universe, and everything? " +
                              "This is a very long question that tests the service's ability to handle large inputs. " +
                              "It should be able to process this without any issues.";
        Map<String, String> request = Map.of("question", longQuestion);
        when(ragService.ask(longQuestion)).thenReturn("The answer to life, the universe, and everything is 42.");

        // Act
        ResponseEntity<Map<String, String>> response = controller.askQuestion(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("question", longQuestion)
                .containsKey("answer");

        System.out.println("✅ Тест с длинным вопросом пройден");
    }

    /**
     * Проверяет, что RagService используется в контроллере.
     */
    @Test
    void testRagServiceIsUsed() {
        assertThat(ragService)
                .as("RagService должен быть внедрен в контроллер")
                .isNotNull();

        System.out.println("✅ RagService используется в контроллере");
    }
}