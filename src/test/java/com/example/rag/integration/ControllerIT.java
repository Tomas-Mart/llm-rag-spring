package com.example.rag.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.service.DocumentService;
import com.example.rag.service.RagService;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Epic("Интеграционные тесты")
@Feature("Контроллеры")
class ControllerIT extends BaseIntegrationTestWithContainers {

    private static final String SUPPORT_FILE_NAME = "support.txt";
    private static final String TEST_CONTENT = "Spring AI is a framework for building AI applications.";
    private static final String TEST_CONTENT_WITH_BOOT = "Spring AI is a framework for building AI applications with Spring Boot.";
    private static final String METADATA = "support-metadata";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService ingestionService;

    @MockBean
    private RagService ragService;

    @BeforeEach
    void setUp() {
        log.info("🚀 [{}] ControllerIT initialized", getTestName());
    }

    @Test
    @Description("Проверка полного цикла работы: загрузка → вопрос → результат")
    @Story("Полный поток пользователя")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("RAG-001")
    void testFullFlow() throws Exception {
        logTestStart("Testing full user flow");

        var file = createTextFile(SUPPORT_FILE_NAME, TEST_CONTENT);
        doNothing().when(ingestionService).ingestDocument(any(MockMultipartFile.class), anyString());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("metadata", METADATA))
                .andExpect(status().isOk());

        var question = "What is Spring AI?";
        when(ragService.ask(question)).thenReturn(TEST_CONTENT);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        logTestSuccess("Full user flow completed");
    }

    @Test
    @Description("Проверка загрузки пустого файла")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-002")
    void testUploadEmptyFile() throws Exception {
        logTestStart("Testing empty file upload");

        var emptyFile = createTextFile("empty.txt", "");

        mockMvc.perform(multipart("/api/documents")
                        .file(emptyFile)
                        .param("metadata", "empty-file"))
                .andExpect(status().isBadRequest());

        logTestSuccess("Empty file handled correctly");
    }

    @Test
    @Description("Проверка задания вопроса без документа")
    @Story("Работа с вопросами")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-003")
    void testAskQuestionWithoutDocument() throws Exception {
        logTestStart("Testing question without document");

        var question = "What is RAG?";
        var expectedAnswer = "RAG is Retrieval-Augmented Generation.";
        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isOk());

        logTestSuccess("Question without document handled");
    }

    @Test
    @Description("Проверка загрузки документа и множественных вопросов")
    @Story("Множественные запросы")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-004")
    void testUploadAndAskMultipleQuestions() throws Exception {
        logTestStart("Testing multiple questions");

        var file = createTextFile(SUPPORT_FILE_NAME, TEST_CONTENT_WITH_BOOT);
        doNothing().when(ingestionService).ingestDocument(any(MockMultipartFile.class), anyString());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("metadata", "support"))
                .andExpect(status().isOk());

        var question1 = "What is Spring AI?";
        when(ragService.ask(question1)).thenReturn(TEST_CONTENT);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question1 + "\"}"))
                .andExpect(status().isOk());

        var question2 = "What is Spring Boot?";
        var answer2 = "Spring Boot is a framework for building microservices.";
        when(ragService.ask(question2)).thenReturn(answer2);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question2 + "\"}"))
                .andExpect(status().isOk());

        logTestSuccess("Multiple questions handled");
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

        var file = new MockMultipartFile(
                "file",
                "document.exe",
                "application/octet-stream",
                "invalid content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("metadata", "invalid-type"))
                .andExpect(status().isOk());

        logTestSuccess("Invalid file type upload handled (accepted)");
    }

    @Test
    @Description("Проверка удаления документа")
    @Story("Управление документами")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-007")
    void testDeleteDocument() throws Exception {
        logTestStart("Testing document deletion");

        var documentId = 1L;

        // ИСПРАВЛЕНО: when(...).thenReturn(...) вместо doNothing()
        when(ingestionService.deleteDocument(anyLong())).thenReturn(true);

        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isOk());

        logTestSuccess("Document deletion works");
    }

    @Test
    @Description("Проверка удаления несуществующего документа")
    @Story("Управление документами")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-008")
    void testDeleteNonExistentDocument() throws Exception {
        logTestStart("Testing deletion of non-existent document");

        var documentId = 999L;

        // Мокаем возврат false (документ не найден)
        when(ingestionService.deleteDocument(anyLong())).thenReturn(false);

        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isNotFound());

        logTestSuccess("Non-existent document deletion handled");
    }

    private MockMultipartFile createTextFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes()
        );
    }
}