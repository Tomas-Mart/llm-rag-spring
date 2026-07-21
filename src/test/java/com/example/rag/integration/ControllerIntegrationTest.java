package com.example.rag.integration;

import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import com.example.rag.support.BaseIntegrationTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Интеграционный тест для проверки работы контроллеров.
 * Проверяет полный цикл работы приложения: загрузка документа, задание вопроса, отображение страниц.
 *
 * <p>Тестируемые сценарии:
 * <ul>
 *   <li>Полный поток работы пользователя</li>
 *   <li>Загрузка документа</li>
 *   <li>Задание вопроса</li>
 *   <li>Отображение главной страницы</li>
 *   <li>Обработка пустых файлов</li>
 *   <li>Множественные запросы</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Epic("Интеграционные тесты")
@Feature("Контроллеры")
@MockitoSettings(strictness = Strictness.LENIENT)
class ControllerIntegrationTest extends BaseIntegrationTest {

    /**
     * Проверяет полный поток работы: загрузка документа, задание вопроса, отображение главной страницы.
     *
     * @throws Exception если ошибка при выполнении запросов
     */
    @Test
    @Description("Проверка полного цикла работы: загрузка документа → задание вопроса → отображение результата")
    @Story("Полный поток пользователя")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("RAG-001")
    void testFullFlow() throws Exception {
        // 1. Загружаем документ
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "support.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Spring AI is a framework for building AI applications.".getBytes()
        );
        doNothing().when(ingestionService).ingestDocument(any(), anyString());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("metadata", "support-metadata"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // 2. Задаем вопрос
        String question = "What is Spring AI?";
        String expectedAnswer = "Spring AI is a framework for building AI applications.";
        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param("question", question))
                .andExpect(status().isOk())
                .andExpect(model().attribute("question", question))
                .andExpect(model().attribute("answer", expectedAnswer));

        // 3. Проверяем главную страницу
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("question"))
                .andExpect(model().attributeExists("answer"));

        System.out.println("✅ Интеграционный тест пройден");
    }

    /**
     * Проверяет загрузку документа с пустым файлом.
     *
     * @throws Exception если ошибка при выполнении запроса
     */
    @Test
    @Description("Проверка загрузки пустого файла")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-002")
    void testUploadDocumentWithEmptyFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );
        doNothing().when(ingestionService).ingestDocument(any(), anyString());

        // Act & Assert
        mockMvc.perform(multipart("/api/documents/upload")
                        .file(emptyFile)
                        .param("metadata", "empty-file"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        System.out.println("✅ Тест загрузки пустого файла пройден");
    }

    /**
     * Проверяет задание вопроса без предварительной загрузки документа.
     *
     * @throws Exception если ошибка при выполнении запроса
     */
    @Test
    @Description("Проверка задания вопроса без предварительной загрузки документа")
    @Story("Работа с вопросами")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-003")
    void testAskQuestionWithoutDocument() throws Exception {
        // Arrange
        String question = "What is RAG?";
        String expectedAnswer = "RAG is Retrieval-Augmented Generation.";
        when(ragService.ask(question)).thenReturn(expectedAnswer);

        // Act & Assert
        mockMvc.perform(post("/ask")
                        .param("question", question))
                .andExpect(status().isOk())
                .andExpect(model().attribute("question", question))
                .andExpect(model().attribute("answer", expectedAnswer));

        System.out.println("✅ Тест вопроса без документа пройден");
    }

    /**
     * Проверяет загрузку документа и задание множественных вопросов.
     *
     * @throws Exception если ошибка при выполнении запросов
     */
    @Test
    @Description("Проверка загрузки документа и задания множественных вопросов")
    @Story("Множественные запросы")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-004")
    void testUploadAndAskMultipleQuestions() throws Exception {
        // 1. Загружаем документ - ожидаем редирект (302)
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "support.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Spring AI is a framework for building AI applications with Spring Boot.".getBytes()
        );
        doNothing().when(ingestionService).ingestDocument(any(), anyString());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("metadata", "support"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // 2. Задаем первый вопрос
        String question1 = "What is Spring AI?";
        String answer1 = "Spring AI is a framework for building AI applications.";
        when(ragService.ask(question1)).thenReturn(answer1);

        mockMvc.perform(post("/ask")
                        .param("question", question1))
                .andExpect(status().isOk())
                .andExpect(model().attribute("answer", answer1));

        // 3. Задаем второй вопрос
        String question2 = "What is Spring Boot?";
        String answer2 = "Spring Boot is a framework for building microservices.";
        when(ragService.ask(question2)).thenReturn(answer2);

        mockMvc.perform(post("/ask")
                        .param("question", question2))
                .andExpect(status().isOk())
                .andExpect(model().attribute("answer", answer2));

        System.out.println("✅ Тест множественных вопросов пройден");
    }

    /**
     * Проверяет, что главная страница имеет пустые поля.
     *
     * @throws Exception если ошибка при выполнении запроса
     */
    @Test
    @Description("Проверка, что главная страница имеет пустые поля")
    @Story("Отображение страниц")
    @Severity(SeverityLevel.MINOR)
    @TmsLink("RAG-005")
    void testMainPageHasEmptyFields() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("question", ""))
                .andExpect(model().attribute("answer", ""));

        System.out.println("✅ Тест пустых полей на главной странице пройден");
    }
}