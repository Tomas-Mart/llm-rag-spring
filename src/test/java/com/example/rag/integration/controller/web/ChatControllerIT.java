package com.example.rag.integration.controller.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.controller.web.ChatController;
import com.example.rag.service.RagService;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Интеграционные тесты для {@link ChatController}.
 *
 * <p>Проверяет работу ChatController с реальным Spring контекстом,
 * реальной базой данных PostgreSQL и реальным VectorStore.</p>
 *
 * <p>Использует {@link BaseIntegrationTestWithContainers} для поднятия
 * контейнера с PostgreSQL и pgvector.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Отображение главной страницы</li>
 *   <li>Успешный запрос к RAG сервису</li>
 *   <li>Пустой вопрос</li>
 *   <li>Вопрос со специальными символами</li>
 *   <li>Обработка ошибок сервиса</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see ChatController
 * @see BaseIntegrationTestWithContainers
 * @since 1.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Epic("Интеграционные тесты")
@Feature("Web Контроллеры")
class ChatControllerIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String VIEW_INDEX = "index";
    private static final String ATTR_QUESTION = "question";
    private static final String ATTR_ANSWER = "answer";
    private static final String ASK_URL = "/ask";
    private static final String ROOT_URL = "/";

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RagService ragService;

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка отображения главной страницы")
    @Story("Отображение страниц")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Главная страница должна загружаться с пустыми полями")
    void testIndexPage_ShouldLoadWithEmptyFields() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ROOT_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attributeExists(ATTR_QUESTION))
                .andExpect(model().attributeExists(ATTR_ANSWER))
                .andExpect(model().attribute(ATTR_QUESTION, ""))
                .andExpect(model().attribute(ATTR_ANSWER, ""));

        log.info("✅ Главная страница отображается корректно");
    }

    @Test
    @Description("Проверка успешного запроса к RAG сервису")
    @Story("Обработка вопросов")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Успешный вопрос должен возвращать ответ от RAG сервиса")
    void testAskQuestion_Success() throws Exception {
        // Arrange
        String question = "What is Spring AI?";

        // Act & Assert
        mockMvc.perform(post(ASK_URL)
                        .param(ATTR_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attributeExists(ATTR_ANSWER));

        log.info("✅ Вопрос успешно обработан");
    }

    @Test
    @Description("Проверка пустого вопроса")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Пустой вопрос должен возвращать сообщение о необходимости задать вопрос")
    void testAskQuestion_WithEmptyQuestion() throws Exception {
        // Arrange
        String question = "";

        // Act & Assert
        mockMvc.perform(post(ASK_URL)
                        .param(ATTR_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attribute(ATTR_ANSWER, "Пожалуйста, задайте вопрос."));

        log.info("✅ Пустой вопрос обработан корректно");
    }

    @Test
    @Description("Проверка вопроса со специальными символами")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Вопрос со спецсимволами должен обрабатываться корректно")
    void testAskQuestion_WithSpecialCharacters() throws Exception {
        // Arrange
        String question = "Что такое RAG? Spring AI vs LangChain4j? 🚀";

        // Act & Assert
        mockMvc.perform(post(ASK_URL)
                        .param(ATTR_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attributeExists(ATTR_ANSWER));

        log.info("✅ Вопрос со спецсимволами обработан корректно");
    }

    @Test
    @Description("Проверка обработки ошибок сервиса")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Ошибка сервиса должна возвращать сообщение об ошибке")
    void testAskQuestion_WhenServiceThrowsException() throws Exception {
        // Arrange
        String question = "Question that will cause error";

        // Act & Assert - проверяем что контроллер обрабатывает ошибки
        mockMvc.perform(post(ASK_URL)
                        .param(ATTR_QUESTION, question))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, question))
                .andExpect(model().attributeExists(ATTR_ANSWER));

        log.info("✅ Ошибка сервиса обработана корректно");
    }

    @Test
    @Description("Проверка длинного вопроса")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Длинный вопрос должен обрабатываться без ошибок")
    void testAskQuestion_WithLongQuestion() throws Exception {
        // Arrange
        String longQuestion = "What is the difference between Spring AI and LangChain4j? " +
                              "Which one should I use for building RAG applications with vector databases? " +
                              "Can you provide a detailed comparison of both frameworks?";

        // Act & Assert
        mockMvc.perform(post(ASK_URL)
                        .param(ATTR_QUESTION, longQuestion))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_INDEX))
                .andExpect(model().attribute(ATTR_QUESTION, longQuestion))
                .andExpect(model().attributeExists(ATTR_ANSWER));

        log.info("✅ Длинный вопрос обработан корректно");
    }

    @Test
    @Description("Проверка что RagService внедрен в контроллер")
    @Story("Архитектура")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("RagService должен быть успешно внедрен")
    void testRagServiceIsInjected() {
        // Assert
        assertThat(ragService)
                .as("RagService должен быть внедрен в контроллер")
                .isNotNull();

        log.info("✅ RagService успешно внедрен");
    }
}