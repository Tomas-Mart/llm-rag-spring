package com.example.rag.unit.controller.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.controller.api.DocumentApiController;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.service.DocumentService;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Модульные тесты для {@link DocumentApiController}.
 *
 * <p>Использует {@link WebMvcTest} для загрузки только web-слоя.
 * {@link DocumentService} замокан через {@link MockBean}.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Успешная загрузка документа</li>
 *   <li>Загрузка с null метаданными</li>
 *   <li>Пустой файл</li>
 *   <li>Файл слишком большого размера</li>
 *   <li>Ошибка при загрузке (DocumentIngestionException)</li>
 *   <li>Общая ошибка при загрузке</li>
 *   <li>Успешное удаление документа</li>
 *   <li>Удаление несуществующего документа</li>
 *   <li>Ошибка при удалении</li>
 *   <li>Проверка существования документа (true)</li>
 *   <li>Проверка существования документа (false)</li>
 *   <li>Ошибка при проверке существования</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see DocumentApiController
 * @since 1.0
 */
@Slf4j
@WebMvcTest(DocumentApiController.class)
@Tag("unit")
@DisplayName("Модульные тесты DocumentApiController")
class DocumentApiControllerTest {

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService ingestionService;

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String DOCUMENTS_URL = "/api/documents";
    private static final String DELETE_URL = "/api/documents/{id}";
    private static final String EXISTS_URL = "/api/documents/exists";
    private static final String FILE_NAME = "test.txt";
    private static final String FILE_CONTENT = "Test content";
    private static final String METADATA = "test-metadata";
    private static final Long DOCUMENT_ID = 1L;
    private static final Long NON_EXISTENT_ID = 999L;

    private MockMultipartFile file;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        file = new MockMultipartFile(
                "file",
                FILE_NAME,
                "text/plain",
                FILE_CONTENT.getBytes()
        );
        log.info("🚀 Running DocumentApiControllerTest");
    }

    // ============================================================
    // ТЕСТЫ - ЗАГРУЗКА ДОКУМЕНТА
    // ============================================================

    @Test
    @DisplayName("Успешная загрузка документа")
    void testUploadDocument_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart(DOCUMENTS_URL)
                        .file(file)
                        .param("metadata", METADATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                .andExpect(jsonPath("$.fileName").value(FILE_NAME))
                .andExpect(jsonPath("$.size").exists());

        verify(ingestionService).ingestDocument(any(), anyString());
        log.info("✅ Тест успешной загрузки документа пройден");
    }

    @Test
    @DisplayName("Загрузка документа с null метаданными")
    void testUploadDocument_WithNullMetadata() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart(DOCUMENTS_URL)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                .andExpect(jsonPath("$.fileName").value(FILE_NAME))
                .andExpect(jsonPath("$.size").exists());

        verify(ingestionService).ingestDocument(any(), any());
        log.info("✅ Тест загрузки с null метаданными пройден");
    }

    @Test
    @DisplayName("Пустой файл - возвращает 400")
    void testUploadDocument_WithEmptyFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart(DOCUMENTS_URL)
                        .file(emptyFile)
                        .param("metadata", METADATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Файл не выбран или пустой"));

        verify(ingestionService, never()).ingestDocument(any(), any());
        log.info("✅ Тест пустого файла пройден");
    }

    @Test
    @DisplayName("Файл слишком большого размера - возвращает 400")
    void testUploadDocument_WithFileTooLarge() throws Exception {
        // Arrange
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent
        );

        // Act & Assert
        mockMvc.perform(multipart(DOCUMENTS_URL)
                        .file(largeFile)
                        .param("metadata", METADATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Размер файла превышает 10MB"));

        verify(ingestionService, never()).ingestDocument(any(), any());
        log.info("✅ Тест превышения размера файла пройден");
    }

    @Test
    @DisplayName("Ошибка DocumentIngestionException - возвращает 400")
    void testUploadDocument_WhenIngestionFails() throws Exception {
        // Arrange
        doThrow(new DocumentIngestionException("Document already exists"))
                .when(ingestionService).ingestDocument(any(), any());

        // Act & Assert
        mockMvc.perform(multipart(DOCUMENTS_URL)
                        .file(file)
                        .param("metadata", METADATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Document already exists"));

        verify(ingestionService).ingestDocument(any(), any());
        log.info("✅ Тест ошибки загрузки документа пройден");
    }

    @Test
    @DisplayName("Общая ошибка при загрузке - возвращает 500")
    void testUploadDocument_WhenGeneralExceptionOccurs() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Something went wrong"))
                .when(ingestionService).ingestDocument(any(), any());

        // Act & Assert
        mockMvc.perform(multipart(DOCUMENTS_URL)
                        .file(file)
                        .param("metadata", METADATA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Внутренняя ошибка сервера: Something went wrong"));

        verify(ingestionService).ingestDocument(any(), any());
        log.info("✅ Тест общей ошибки загрузки документа пройден");
    }

    // ============================================================
    // ТЕСТЫ - УДАЛЕНИЕ ДОКУМЕНТА
    // ============================================================

    @Test
    @DisplayName("Успешное удаление документа")
    void testDeleteDocument_Success() throws Exception {
        // Arrange
        when(ingestionService.deleteDocument(DOCUMENT_ID)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete(DELETE_URL, DOCUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document deleted successfully"))
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID));

        verify(ingestionService).deleteDocument(DOCUMENT_ID);
        log.info("✅ Тест успешного удаления документа пройден");
    }

    @Test
    @DisplayName("Удаление несуществующего документа - возвращает 404")
    void testDeleteDocument_NotFound() throws Exception {
        // Arrange
        when(ingestionService.deleteDocument(NON_EXISTENT_ID)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete(DELETE_URL, NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Документ с ID " + NON_EXISTENT_ID + " не найден"));

        verify(ingestionService).deleteDocument(NON_EXISTENT_ID);
        log.info("✅ Тест удаления несуществующего документа пройден");
    }

    @Test
    @DisplayName("Ошибка при удалении (RuntimeException с not found) - возвращает 404")
    void testDeleteDocument_WithRuntimeExceptionNotFound() throws Exception {
        // Arrange
        when(ingestionService.deleteDocument(DOCUMENT_ID))
                .thenThrow(new RuntimeException("Document not found"));

        // Act & Assert
        mockMvc.perform(delete(DELETE_URL, DOCUMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Документ с ID " + DOCUMENT_ID + " не найден"));

        verify(ingestionService).deleteDocument(DOCUMENT_ID);
        log.info("✅ Тест удаления документа с RuntimeException (not found) пройден");
    }

    @Test
    @DisplayName("Ошибка при удалении (RuntimeException без not found) - возвращает 500")
    void testDeleteDocument_WithRuntimeException() throws Exception {
        // Arrange
        String errorMessage = "Database connection error";
        when(ingestionService.deleteDocument(DOCUMENT_ID))
                .thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        mockMvc.perform(delete(DELETE_URL, DOCUMENT_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Ошибка удаления: " + errorMessage));

        verify(ingestionService).deleteDocument(DOCUMENT_ID);
        log.info("✅ Тест удаления документа с RuntimeException пройден");
    }

    @Test
    @DisplayName("Ошибка при удалении (общая ошибка) - возвращает 500")
    void testDeleteDocument_WithGeneralException() throws Exception {
        // Arrange
        String errorMessage = "Database error";
        when(ingestionService.deleteDocument(DOCUMENT_ID))
                .thenThrow(new IllegalStateException(errorMessage));

        // Act & Assert
        mockMvc.perform(delete(DELETE_URL, DOCUMENT_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Ошибка удаления: " + errorMessage));

        verify(ingestionService).deleteDocument(DOCUMENT_ID);
        log.info("✅ Тест удаления документа с общей ошибкой пройден");
    }

    // ============================================================
    // ТЕСТЫ - ПРОВЕРКА СУЩЕСТВОВАНИЯ ДОКУМЕНТА
    // ============================================================

    @Test
    @DisplayName("Проверка существования документа (true)")
    void testDocumentExists_WhenExists() throws Exception {
        // Arrange
        when(ingestionService.documentExists(FILE_NAME)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get(EXISTS_URL)
                        .param("fileName", FILE_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value(FILE_NAME))
                .andExpect(jsonPath("$.exists").value(true));

        verify(ingestionService).documentExists(FILE_NAME);
        log.info("✅ Тест проверки существования документа (true) пройден");
    }

    @Test
    @DisplayName("Проверка существования документа (false)")
    void testDocumentExists_WhenNotExists() throws Exception {
        // Arrange
        String nonExistentFile = "nonexistent.txt";
        when(ingestionService.documentExists(nonExistentFile)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get(EXISTS_URL)
                        .param("fileName", nonExistentFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value(nonExistentFile))
                .andExpect(jsonPath("$.exists").value(false));

        verify(ingestionService).documentExists(nonExistentFile);
        log.info("✅ Тест проверки существования документа (false) пройден");
    }

    @Test
    @DisplayName("Ошибка при проверке существования - возвращает 500")
    void testDocumentExists_WithException() throws Exception {
        // Arrange
        String errorMessage = "Database error";
        when(ingestionService.documentExists(FILE_NAME))
                .thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        mockMvc.perform(get(EXISTS_URL)
                        .param("fileName", FILE_NAME))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Ошибка проверки: " + errorMessage));

        verify(ingestionService).documentExists(FILE_NAME);
        log.info("✅ Тест проверки существования документа с ошибкой пройден");
    }
}