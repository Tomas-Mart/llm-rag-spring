package com.example.rag.controller.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.service.RagService;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Модульный тест для проверки работы {@link ChatController}.
 * <p>
 * Использует {@link WebMvcTest} для загрузки только web-слоя.
 * {@link RagService} замокан через {@link MockBean}.
 *
 * @author RAG Application Team
 * @version 6.0
 * @see ChatController
 * @since 1.0
 */
@Slf4j
@WebMvcTest(ChatController.class)
@Epic("Модульные тесты")
@Feature("Контроллер чата")
class ChatControllerTest {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String VIEW_INDEX = "index";
    private static final String ATTR_QUESTION = "question";
    private static final String ATTR_ANSWER = "answer";
    private static final String PARAM_QUESTION = "question";
    private static final String ERROR_MESSAGE = "Извините, произошла ошибка: ";

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagService ragService;

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка отображения главной страницы")
    @Story("Отображение страниц")
    @Severity(SeverityLevel.CRITICAL)
    void testIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attributeExists(ATTR_QUESTION))
                .andExpect(model().attributeExists(ATTR_ANSWER));
    }

    @Test
    @Description("Проверка задания вопроса")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.CRITICAL)
    void testAskQuestion() throws Exception {
        var question = "What is Spring AI?";
        var expectedAnswer = "Spring AI is a framework for building AI applications.";

        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, expectedAnswer));

        verify(ragService).ask(question);
    }

    @Test
    @Description("Проверка задания пустого вопроса")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestionWithEmptyQuestion() throws Exception {
        var question = "";
        var expectedAnswer = "Пожалуйста, задайте вопрос.";

        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, expectedAnswer));

        verify(ragService).ask(question);
    }

    @Test
    @Description("Проверка задания длинного вопроса")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestionWithLongQuestion() throws Exception {
        var question = "What is the difference between " +
                       "Spring AI and LangChain4j? Which one should I use for " +
                       "building RAG applications with vector databases?";

        var expectedAnswer = "Both frameworks can be used, but Spring AI is more integrated with Spring ecosystem.";

        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, expectedAnswer));

        verify(ragService).ask(question);
    }

    @Test
    @Description("Проверка задания вопроса со специальными символами")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestionWithSpecialCharacters() throws Exception {
        var question = "Что такое RAG? Spring AI vs LangChain4j? 🚀";
        var expectedAnswer = "RAG - это Retrieval-Augmented Generation.";

        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, expectedAnswer));

        verify(ragService).ask(question);
    }

    @Test
    @Description("Проверка обработки ошибки сервиса")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.CRITICAL)
    void testAskQuestionWhenServiceThrowsException() throws Exception {
        var question = "Test question";
        var errorMessage = "Service error";

        when(ragService.ask(question)).thenThrow(new RuntimeException(errorMessage));

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, "❌ " + ERROR_MESSAGE + errorMessage));

        verify(ragService).ask(question);
    }
}