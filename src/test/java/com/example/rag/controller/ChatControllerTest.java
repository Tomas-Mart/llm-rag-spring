package com.example.rag.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import com.example.rag.service.RagService;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Модульный тест для проверки работы {@link ChatController}.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет работу контроллера чата: отображение страниц, обработку вопросов,
 * валидацию ввода и обработку ошибок.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Отображение главной страницы</li>
 *   <li>Задание вопроса (обычный, пустой, длинный, со спецсимволами)</li>
 *   <li>Обработка ошибок сервиса</li>
 * </ul>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Использует {@link MockMvc} для тестирования контроллера</li>
 *   <li>Все зависимости замоканы через {@link MockitoExtension}</li>
 *   <li>Проверяет как HTTP ответы, так и взаимодействие с моделью</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see ChatController
 * @see RagService
 * @since 1.0
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
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
    // МОКИ
    // ============================================================

    @Mock
    private RagService ragService;

    @Mock
    private Model model;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
        log.info("✅ ChatControllerTest initialized");
    }

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка отображения главной страницы")
    @Story("Отображение страниц")
    @Severity(SeverityLevel.CRITICAL)
    void testIndexPage() throws Exception {
        logTestStart("Testing index page");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attributeExists(ATTR_QUESTION))
                .andExpect(model().attributeExists(ATTR_ANSWER));

        logTestSuccess("Index page displayed correctly");
    }

    @Test
    @Description("Проверка атрибутов модели на главной странице")
    @Story("Отображение страниц")
    @Severity(SeverityLevel.NORMAL)
    void testIndexPageWithModelAttributes() {
        logTestStart("Testing model attributes");

        String viewName = chatController.index(model);

        assertThat(viewName)
                .as("View name should be 'index'")
                .isEqualTo(VIEW_INDEX);

        verify(model).addAttribute(ATTR_QUESTION, "");
        verify(model).addAttribute(ATTR_ANSWER, "");

        logTestSuccess("Model attributes verified");
    }

    @Test
    @Description("Проверка задания вопроса")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.CRITICAL)
    void testAskQuestion() throws Exception {
        logTestStart("Testing ask question");

        String question = "What is Spring AI?";
        String expectedAnswer = "Spring AI is a framework for building AI applications.";

        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, expectedAnswer));

        verify(ragService).ask(question);

        logTestSuccess("Question processed correctly");
    }

    @Test
    @Description("Проверка задания пустого вопроса")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestionWithEmptyQuestion() throws Exception {
        logTestStart("Testing empty question");

        String question = "";
        String expectedAnswer = "Пожалуйста, задайте вопрос.";

        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, expectedAnswer));

        verify(ragService).ask(question);

        logTestSuccess("Empty question handled correctly");
    }

    @Test
    @Description("Проверка задания длинного вопроса")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestionWithLongQuestion() throws Exception {
        logTestStart("Testing long question");

        String question = "What is the difference between " +
                          "Spring AI and LangChain4j? Which one should I use for " +
                          "building RAG applications with vector databases?";

        String expectedAnswer = "Both frameworks can be used, but Spring AI is more integrated with Spring ecosystem.";

        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, expectedAnswer));

        verify(ragService).ask(question);

        logTestSuccess("Long question handled correctly");
    }

    @Test
    @Description("Проверка задания вопроса со специальными символами")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.NORMAL)
    void testAskQuestionWithSpecialCharacters() throws Exception {
        logTestStart("Testing question with special characters");

        String question = "Что такое RAG? Spring AI vs LangChain4j? 🚀";
        String expectedAnswer = "RAG - это Retrieval-Augmented Generation.";

        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, expectedAnswer));

        verify(ragService).ask(question);

        logTestSuccess("Special characters handled correctly");
    }

    @Test
    @Description("Проверка обработки ошибки сервиса")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.CRITICAL)
    void testAskQuestionWhenServiceThrowsException() throws Exception {
        logTestStart("Testing service exception handling");

        String question = "Test question";
        String errorMessage = "Service error";

        when(ragService.ask(question)).thenThrow(new RuntimeException(errorMessage));

        mockMvc.perform(post("/ask")
                        .param(PARAM_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, ERROR_MESSAGE + errorMessage));

        verify(ragService).ask(question);

        logTestSuccess("Service exception handled correctly");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Логирует начало теста.
     *
     * @param message сообщение для логирования
     */
    private void logTestStart(String message) {
        log.info("🚀 [{}] {}", getTestName(), message);
    }

    /**
     * Логирует успешное завершение теста.
     *
     * @param message сообщение для логирования
     */
    private void logTestSuccess(String message) {
        log.info("✅ [{}] {}", getTestName(), message);
    }

    /**
     * Возвращает имя теста.
     *
     * @return имя класса теста
     */
    private String getTestName() {
        return getClass().getSimpleName();
    }
}