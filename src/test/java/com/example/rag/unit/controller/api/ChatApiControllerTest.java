package com.example.rag.unit.controller.api;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.controller.api.ChatApiController;
import com.example.rag.service.RagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Модульные тесты для {@link ChatApiController}.
 *
 * <p>Использует {@link WebMvcTest} для загрузки только web-слоя.
 * {@link RagService} замокан через {@link MockBean}.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Успешный ответ на вопрос</li>
 *   <li>Пустой вопрос</li>
 *   <li>Null вопрос</li>
 *   <li>Отсутствующее поле question</li>
 *   <li>Вопрос с пробелами</li>
 *   <li>Ошибка сервиса</li>
 *   <li>Длинный вопрос</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see ChatApiController
 * @since 1.0
 */
@Slf4j
@WebMvcTest(ChatApiController.class)
@Tag("unit")
@DisplayName("Модульные тесты ChatApiController")
class ChatApiControllerTest {

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagService ragService;

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String CHAT_URL = "/api/chat";
    private static final String QUESTION_KEY = "question";
    private static final String ANSWER_KEY = "answer";
    private static final String ERROR_KEY = "error";
    private static final String TEST_QUESTION = "What is Spring AI?";
    private static final String TEST_ANSWER = "Spring AI is a framework for building AI applications.";

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        log.info("🚀 Running ChatApiControllerTest");
    }

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @DisplayName("Успешный ответ на вопрос")
    void testAskQuestion_Success() throws Exception {
        // Arrange
        Map<String, String> request = Map.of(QUESTION_KEY, TEST_QUESTION);
        when(ragService.ask(TEST_QUESTION)).thenReturn(TEST_ANSWER);

        // Act & Assert
        mockMvc.perform(post(CHAT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + QUESTION_KEY).value(TEST_QUESTION))
                .andExpect(jsonPath("$." + ANSWER_KEY).value(TEST_ANSWER));

        verify(ragService).ask(TEST_QUESTION);
        log.info("✅ Тест успешного ответа на вопрос пройден");
    }

    @Test
    @DisplayName("Пустой вопрос - возвращает 400")
    void testAskQuestion_WithEmptyQuestion() throws Exception {
        // Arrange
        Map<String, String> request = Map.of(QUESTION_KEY, "");

        // Act & Assert
        mockMvc.perform(post(CHAT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$." + ERROR_KEY).value("Пожалуйста, задайте вопрос."));

        log.info("✅ Тест с пустым вопросом пройден");
    }

    @Test
    @DisplayName("Null вопрос - возвращает 400")
    void testAskQuestion_WithNullQuestion() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put(QUESTION_KEY, null);

        // Act & Assert
        mockMvc.perform(post(CHAT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$." + ERROR_KEY).value("Пожалуйста, задайте вопрос."));

        log.info("✅ Тест с null вопросом пройден");
    }

    @Test
    @DisplayName("Отсутствующее поле question - возвращает 400")
    void testAskQuestion_WithMissingQuestionField() throws Exception {
        // Arrange
        Map<String, String> request = Map.of("wrongField", "some value");

        // Act & Assert
        mockMvc.perform(post(CHAT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$." + ERROR_KEY).value("Пожалуйста, задайте вопрос."));

        log.info("✅ Тест с отсутствующим полем question пройден");
    }

    @Test
    @DisplayName("Null запрос - возвращает 400")
    void testAskQuestion_WithNullRequest() throws Exception {
        // Act & Assert - передаем пустой объект вместо "null"
        mockMvc.perform(post(CHAT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))  // ← Пустой JSON объект
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Пожалуйста, задайте вопрос."));

        log.info("✅ Тест с null запросом пройден");
    }

    @Test
    @DisplayName("Вопрос с пробелами - успешный ответ")
    void testAskQuestion_WithSpaces() throws Exception {
        // Arrange
        String questionWithSpaces = "   What is Spring AI?   ";
        Map<String, String> request = Map.of(QUESTION_KEY, questionWithSpaces);
        when(ragService.ask(questionWithSpaces)).thenReturn(TEST_ANSWER);

        // Act & Assert
        mockMvc.perform(post(CHAT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + QUESTION_KEY).value(questionWithSpaces))
                .andExpect(jsonPath("$." + ANSWER_KEY).value(TEST_ANSWER));

        verify(ragService).ask(questionWithSpaces);
        log.info("✅ Тест с пробелами в вопросе пройден");
    }

    @Test
    @DisplayName("Ошибка сервиса - возвращает 500")
    void testAskQuestion_WhenServiceThrowsException() throws Exception {
        // Arrange
        Map<String, String> request = Map.of(QUESTION_KEY, TEST_QUESTION);
        String errorMessage = "Service unavailable";
        when(ragService.ask(anyString())).thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        mockMvc.perform(post(CHAT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$." + ERROR_KEY).value(errorMessage));

        verify(ragService).ask(anyString());
        log.info("✅ Тест ошибки сервиса пройден");
    }

    @Test
    @DisplayName("Длинный вопрос - успешный ответ")
    void testAskQuestion_WithLongQuestion() throws Exception {
        // Arrange
        String longQuestion = "What is the meaning of life, the universe, and everything? " +
                              "This is a very long question that tests the service's ability to handle large inputs. " +
                              "It should be able to process this without any issues.";
        String longAnswer = "The answer to life, the universe, and everything is 42.";
        Map<String, String> request = Map.of(QUESTION_KEY, longQuestion);
        when(ragService.ask(longQuestion)).thenReturn(longAnswer);

        // Act & Assert
        mockMvc.perform(post(CHAT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + QUESTION_KEY).value(longQuestion))
                .andExpect(jsonPath("$." + ANSWER_KEY).value(longAnswer));

        verify(ragService).ask(longQuestion);
        log.info("✅ Тест с длинным вопросом пройден");
    }
}