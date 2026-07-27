package com.example.rag.unit.controller.web;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.controller.web.DocumentController;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.service.DocumentService;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Модульный тест для проверки работы {@link DocumentController}.
 * <p>
 * Использует {@link WebMvcTest} для загрузки только web-слоя.
 * {@link DocumentService} замокан через {@link MockBean}.
 *
 * @author RAG Application Team
 * @version 6.0
 * @see DocumentController
 * @since 1.0
 */
@Slf4j
@WebMvcTest(DocumentController.class)
@Epic("Модульные тесты")
@Feature("Контроллер документов")
class DocumentControllerTest {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String UPLOAD_URL = "/api/documents/upload";
    private static final String REDIRECT_URL = "/";
    private static final String FILE_PARAM = "file";
    private static final String METADATA_PARAM = "metadata";
    private static final String FORCE_PARAM = "force";
    private static final String FILE_NAME = "support.txt";
    private static final String FILE_CONTENT = "Test content";
    private static final String DEFAULT_METADATA = "support-metadata";

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService ingestionService;

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка загрузки документа с метаданными")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.CRITICAL)
    void testUploadDocument() throws Exception {
        var file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        when(ingestionService.documentExists(FILE_NAME)).thenReturn(false);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, DEFAULT_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService).ingestDocument(any(), anyString());
    }

    @Test
    @Description("Проверка загрузки документа без метаданных")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    void testUploadDocumentWithoutMetadata() throws Exception {
        var file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        when(ingestionService.documentExists(FILE_NAME)).thenReturn(false);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService).ingestDocument(any(), any());
    }

    @Test
    @Description("Проверка загрузки пустого файла")
    @Story("Валидация")
    @Severity(SeverityLevel.CRITICAL)
    void testUploadEmptyFile() throws Exception {
        var emptyFile = new MockMultipartFile(
                FILE_PARAM,
                "empty.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(emptyFile)
                        .param(METADATA_PARAM, DEFAULT_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService, never()).ingestDocument(any(), anyString());
    }

    @Test
    @Description("Проверка загрузки файла больше 10MB")
    @Story("Валидация")
    @Severity(SeverityLevel.CRITICAL)
    void testUploadLargeFile() throws Exception {
        var largeContent = new byte[11 * 1024 * 1024];
        var largeFile = new MockMultipartFile(
                FILE_PARAM,
                "large.txt",
                MediaType.TEXT_PLAIN_VALUE,
                largeContent
        );

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(largeFile)
                        .param(METADATA_PARAM, DEFAULT_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService, never()).ingestDocument(any(), anyString());
    }

    @Test
    @Description("Проверка обработки DocumentIngestionException")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.CRITICAL)
    void testUploadDocumentWhenServiceThrowsDocumentIngestionException() throws Exception {
        var file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        when(ingestionService.documentExists(FILE_NAME)).thenReturn(false);
        doThrow(new DocumentIngestionException("Document error"))
                .when(ingestionService).ingestDocument(any(), anyString());

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, DEFAULT_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService).ingestDocument(any(), anyString());
    }

    @Test
    @Description("Проверка обработки общих исключений")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.NORMAL)
    void testUploadDocumentWhenServiceThrowsException() throws Exception {
        var file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        when(ingestionService.documentExists(FILE_NAME)).thenReturn(false);
        doThrow(new RuntimeException("Processing error"))
                .when(ingestionService).ingestDocument(any(), anyString());

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, DEFAULT_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService).ingestDocument(any(), anyString());
    }

    @Test
    @Description("Проверка принудительной перезагрузки документа")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    void testUploadDocumentWithForce() throws Exception {
        var file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        when(ingestionService.documentExists(FILE_NAME)).thenReturn(true);
        doNothing().when(ingestionService).reIngestDocument(any(), anyString());

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, DEFAULT_METADATA)
                        .param(FORCE_PARAM, "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService).reIngestDocument(any(), anyString());
        verify(ingestionService, never()).ingestDocument(any(), anyString());
    }

    @Test
    @Description("Проверка загрузки существующего документа без force")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    void testUploadExistingDocumentWithoutForce() throws Exception {
        var file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        when(ingestionService.documentExists(FILE_NAME)).thenReturn(true);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, DEFAULT_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService).documentExists(FILE_NAME);
        verify(ingestionService, never()).ingestDocument(any(), anyString());
        verify(ingestionService, never()).reIngestDocument(any(), anyString());
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    private MockMultipartFile createMultipartFile(String fileName, String content) {
        return new MockMultipartFile(
                FILE_PARAM,
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}