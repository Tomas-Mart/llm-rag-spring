package com.example.rag.controller;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.service.DocumentIngestionService;
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
 *
 * <h2>Назначение</h2>
 * <p>Проверяет загрузку документов через REST API, валидацию и обработку ошибок.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Загрузка документа с метаданными</li>
 *   <li>Загрузка документа без метаданных</li>
 *   <li>Загрузка пустого файла (валидация)</li>
 *   <li>Загрузка файла больше 10MB (валидация)</li>
 *   <li>Обработка DocumentIngestionException</li>
 *   <li>Обработка общих исключений</li>
 *   <li>Принудительная перезагрузка документа (force)</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see DocumentController
 * @see DocumentIngestionService
 * @since 1.0
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
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
    // МОКИ
    // ============================================================

    @Mock
    private DocumentIngestionService ingestionService;

    @InjectMocks
    private DocumentController documentController;

    private MockMvc mockMvc;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(documentController)
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .build();
        log.info("✅ DocumentControllerTest initialized");
    }

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка загрузки документа с метаданными")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.CRITICAL)
    void testUploadDocument() throws Exception {
        logTestStart("Testing document upload with metadata");

        MockMultipartFile file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        // Настраиваем мок - документ не существует
        when(ingestionService.documentExists(FILE_NAME)).thenReturn(false);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, DEFAULT_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService).ingestDocument(any(), anyString());

        logTestSuccess("Document upload with metadata completed");
    }

    @Test
    @Description("Проверка загрузки документа без метаданных")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    void testUploadDocumentWithoutMetadata() throws Exception {
        logTestStart("Testing document upload without metadata");

        MockMultipartFile file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        // Настраиваем мок - документ не существует
        when(ingestionService.documentExists(FILE_NAME)).thenReturn(false);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        verify(ingestionService).ingestDocument(any(), any());

        logTestSuccess("Document upload without metadata completed");
    }

    @Test
    @Description("Проверка загрузки пустого файла")
    @Story("Валидация")
    @Severity(SeverityLevel.CRITICAL)
    void testUploadEmptyFile() throws Exception {
        logTestStart("Testing empty file upload");

        MockMultipartFile emptyFile = new MockMultipartFile(
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

        // ✅ Пустой файл НЕ должен вызывать сервис
        verify(ingestionService, never()).ingestDocument(any(), anyString());

        logTestSuccess("Empty file handled correctly");
    }

    @Test
    @Description("Проверка загрузки файла больше 10MB")
    @Story("Валидация")
    @Severity(SeverityLevel.CRITICAL)
    void testUploadLargeFile() throws Exception {
        logTestStart("Testing large file upload");

        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
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

        // ✅ Большой файл НЕ должен вызывать сервис
        verify(ingestionService, never()).ingestDocument(any(), anyString());

        logTestSuccess("Large file handled correctly");
    }

    @Test
    @Description("Проверка обработки DocumentIngestionException")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.CRITICAL)
    void testUploadDocumentWhenServiceThrowsDocumentIngestionException() throws Exception {
        logTestStart("Testing DocumentIngestionException handling");

        MockMultipartFile file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        // Настраиваем мок - документ не существует
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

        logTestSuccess("DocumentIngestionException handled correctly");
    }

    @Test
    @Description("Проверка обработки общих исключений")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.NORMAL)
    void testUploadDocumentWhenServiceThrowsException() throws Exception {
        logTestStart("Testing general exception handling");

        MockMultipartFile file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        // Настраиваем мок - документ не существует
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

        logTestSuccess("General exception handled correctly");
    }

    @Test
    @Description("Проверка принудительной перезагрузки документа")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    void testUploadDocumentWithForce() throws Exception {
        logTestStart("Testing document upload with force flag");

        MockMultipartFile file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        // ✅ Настраиваем мок - документ СУЩЕСТВУЕТ
        when(ingestionService.documentExists(FILE_NAME)).thenReturn(true);
        doNothing().when(ingestionService).reIngestDocument(any(), anyString());

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, DEFAULT_METADATA)
                        .param(FORCE_PARAM, "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        // ✅ Должен вызываться reIngestDocument, а не ingestDocument
        verify(ingestionService).reIngestDocument(any(), anyString());
        verify(ingestionService, never()).ingestDocument(any(), anyString());

        logTestSuccess("Document upload with force completed");
    }

    @Test
    @Description("Проверка загрузки существующего документа без force")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    void testUploadExistingDocumentWithoutForce() throws Exception {
        logTestStart("Testing existing document without force");

        MockMultipartFile file = createMultipartFile(FILE_NAME, FILE_CONTENT);

        // Настраиваем мок - документ СУЩЕСТВУЕТ
        when(ingestionService.documentExists(FILE_NAME)).thenReturn(true);

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, DEFAULT_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists("message"));

        // ✅ Должен вызываться documentExists, но НЕ ingestDocument
        verify(ingestionService).documentExists(FILE_NAME);
        verify(ingestionService, never()).ingestDocument(any(), anyString());
        verify(ingestionService, never()).reIngestDocument(any(), anyString());

        logTestSuccess("Existing document without force handled correctly");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Создает MockMultipartFile для тестирования.
     *
     * @param fileName имя файла
     * @param content  содержимое файла
     * @return MockMultipartFile
     */
    private MockMultipartFile createMultipartFile(String fileName, String content) {
        return new MockMultipartFile(
                FILE_PARAM,
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

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