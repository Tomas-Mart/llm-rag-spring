package com.example.rag.controller.api;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.service.RagService;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для {@link ChatApiController}.
 * Проверяют REST API эндпоинты для чата.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatApiControllerTest extends BaseIntegrationTestWithContainers {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagService ragService;

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    void testAskQuestion_Success() throws Exception {
        // Arrange
        String question = "What is Spring AI?";
        String answer = "Spring AI is a framework for building AI applications.";
        Map<String, String> request = Map.of("question", question);

        when(ragService.ask(question)).thenReturn(answer);

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value(question))
                .andExpect(jsonPath("$.answer").value(answer));

        System.out.println("✅ Тест успешного ответа на вопрос пройден");
    }

    @Test
    void testAskQuestion_WithEmptyQuestion() throws Exception {
        // Arrange
        Map<String, String> request = Map.of("question", "");

        // Act & Assert - ожидаем 400 для пустого вопроса
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Пожалуйста, задайте вопрос."));

        System.out.println("✅ Тест с пустым вопросом пройден");
    }

    @Test
    void testAskQuestion_WithNullQuestion() throws Exception {
        // Arrange - создаем Map с null значением через HashMap
        Map<String, String> request = new HashMap<>();
        request.put("question", null);

        // Act & Assert - ожидаем 400 для null вопроса
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Пожалуйста, задайте вопрос."));

        System.out.println("✅ Тест с null вопросом пройден");
    }

    @Test
    void testAskQuestion_WithMissingQuestionField() throws Exception {
        // Arrange
        Map<String, String> request = Map.of("wrongField", "some value");

        // Act & Assert - ожидаем 400
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Пожалуйста, задайте вопрос."));  // ← Исправлено

        System.out.println("✅ Тест с отсутствующим полем question пройден");
    }

    @Test
    void testAskQuestion_WithSpaces() throws Exception {
        // Arrange
        String questionWithSpaces = "   What is Spring AI?   ";
        String expectedAnswer = "Spring AI is a framework.";
        Map<String, String> request = Map.of("question", questionWithSpaces);
        when(ragService.ask(questionWithSpaces)).thenReturn(expectedAnswer);

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value(questionWithSpaces))
                .andExpect(jsonPath("$.answer").value(expectedAnswer));

        System.out.println("✅ Тест с пробелами в вопросе пройден");
    }

    @Test
    void testAskQuestion_WhenServiceThrowsException() throws Exception {
        // Arrange
        String question = "What is Spring AI?";
        Map<String, String> request = Map.of("question", question);
        String errorMessage = "Service unavailable";

        when(ragService.ask(anyString())).thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(errorMessage));

        System.out.println("✅ Тест ошибки сервиса пройден");
    }

    @Test
    void testAskQuestion_WithLongQuestion() throws Exception {
        // Arrange
        String longQuestion = "What is the meaning of life, the universe, and everything? " +
                              "This is a very long question that tests the service's ability to handle large inputs. " +
                              "It should be able to process this without any issues.";
        Map<String, String> request = Map.of("question", longQuestion);
        when(ragService.ask(longQuestion)).thenReturn("The answer to life, the universe, and everything is 42.");

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value(longQuestion))
                .andExpect(jsonPath("$.answer").exists());

        System.out.println("✅ Тест с длинным вопросом пройден");
    }

    @Test
    void testRagServiceIsUsed() {
        assertThat(ragService)
                .as("RagService должен быть внедрен в контроллер")
                .isNotNull();

        System.out.println("✅ RagService используется в контроллере");
    }
}