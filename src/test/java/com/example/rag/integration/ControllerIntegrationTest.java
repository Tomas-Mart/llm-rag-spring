package com.example.rag.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.service.DocumentIngestionService;
import com.example.rag.service.RagService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Интеграционный тест для проверки работы контроллеров.
 * Проверяет полный цикл работы приложения: загрузка документа, задание вопроса, отображение страниц.
 *
 * @author RAG Application Team
 * @version 2.0
 * @since 1.0
 */
@Epic("Интеграционные тесты")
@Feature("Контроллеры")
@MockitoSettings(strictness = Strictness.LENIENT)
@SpringBootTest
@AutoConfigureMockMvc
class ControllerIntegrationTest extends BaseIntegrationTest {

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    /**
     * MockMvc для выполнения HTTP запросов.
     * Автоматически настраивается Spring Boot.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Мок для DocumentIngestionService.
     */
    @MockBean
    private DocumentIngestionService ingestionService;

    /**
     * Мок для RagService.
     */
    @MockBean
    private RagService ragService;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    /**
     * Настройка перед каждым тестом.
     * Используем logger из BaseIntegrationTest для логирования.
     */
    @BeforeEach
    void setUp() {
        // Используем методы из BaseIntegrationTest
        logTestStart("ControllerIntegrationTest initialization");

        // Используем protected logger из BaseIntegrationTest
        logger.info("   - MockMvc: {}", mockMvc != null ? "available" : "null");
        logger.info("   - ingestionService: {}", ingestionService != null ? "available" : "null");
        logger.info("   - ragService: {}", ragService != null ? "available" : "null");
    }

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка полного цикла работы: загрузка документа → задание вопроса → отображение результата")
    @Story("Полный поток пользователя")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("RAG-001")
    void testFullFlow() throws Exception {
        logTestStart("Testing full user flow");

        // 1. Создаем файл для загрузки
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "support.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Spring AI is a framework for building AI applications.".getBytes()
        );

        // Мокаем метод ingestDocument - правильная сигнатура: (MultipartFile, String)
        doNothing().when(ingestionService).ingestDocument(any(MockMultipartFile.class), anyString());

        // Выполняем загрузку
        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("metadata", "support-metadata"))
                .andExpect(status().isOk());

        // 2. Задаем вопрос
        String question = "What is Spring AI?";
        String expectedAnswer = "Spring AI is a framework for building AI applications.";
        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isOk());

        // 3. Проверяем главную страницу
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        logTestSuccess("Full user flow completed successfully");
    }

    @Test
    @Description("Проверка загрузки пустого файла")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-002")
    void testUploadDocumentWithEmptyFile() throws Exception {
        logTestStart("Testing empty file upload");

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(emptyFile)
                        .param("metadata", "empty-file"))
                .andExpect(status().isBadRequest());

        logTestSuccess("Empty file handled correctly");
    }

    @Test
    @Description("Проверка задания вопроса без предварительной загрузки документа")
    @Story("Работа с вопросами")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-003")
    void testAskQuestionWithoutDocument() throws Exception {
        logTestStart("Testing question without document");

        String question = "What is RAG?";
        String expectedAnswer = "RAG is Retrieval-Augmented Generation.";
        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isOk());

        logTestSuccess("Question without document handled correctly");
    }

    @Test
    @Description("Проверка загрузки документа и задания множественных вопросов")
    @Story("Множественные запросы")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-004")
    void testUploadAndAskMultipleQuestions() throws Exception {
        logTestStart("Testing multiple questions");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "support.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Spring AI is a framework for building AI applications with Spring Boot.".getBytes()
        );
        doNothing().when(ingestionService).ingestDocument(any(MockMultipartFile.class), anyString());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("metadata", "support"))
                .andExpect(status().isOk());

        String question1 = "What is Spring AI?";
        String answer1 = "Spring AI is a framework for building AI applications.";
        when(ragService.ask(question1)).thenReturn(answer1);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question1 + "\"}"))
                .andExpect(status().isOk());

        String question2 = "What is Spring Boot?";
        String answer2 = "Spring Boot is a framework for building microservices.";
        when(ragService.ask(question2)).thenReturn(answer2);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question2 + "\"}"))
                .andExpect(status().isOk());

        logTestSuccess("Multiple questions handled correctly");
    }

    @Test
    @Description("Проверка доступности главной страницы")
    @Story("Отображение страниц")
    @Severity(SeverityLevel.MINOR)
    @TmsLink("RAG-005")
    void testMainPageIsAccessible() throws Exception {
        logTestStart("Testing main page accessibility");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        logTestSuccess("Main page is accessible");
    }

    @Test
    @Description("Проверка загрузки документа с неверным типом файла")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-006")
    void testUploadInvalidFileType() throws Exception {
        logTestStart("Testing invalid file type upload");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.exe",
                "application/octet-stream",
                "invalid content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("metadata", "invalid-type"))
                .andExpect(status().isBadRequest());

        logTestSuccess("Invalid file type handled correctly");
    }

    @Test
    @Description("Проверка удаления документа")
    @Story("Управление документами")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-007")
    void testDeleteDocument() throws Exception {
        logTestStart("Testing document deletion");

        Long documentId = 1L;

        // Мокаем удаление через reIngestDocument
        doNothing().when(ingestionService).reIngestDocument(any(MockMultipartFile.class), anyString());

        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isOk());

        logTestSuccess("Document deletion works correctly");
    }
}