package com.example.rag.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.service.DocumentService;
import com.example.rag.support.BaseIntegrationTestWithContainers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для {@link DocumentApiController}.
 * Проверяют REST API эндпоинты для работы с документами.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DocumentApiControllerTest extends BaseIntegrationTestWithContainers {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService ingestionService;

    // ============================================================
    // ТЕСТЫ ДЛЯ uploadDocument
    // ============================================================

    @Test
    void testUploadDocument_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Test content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("metadata", "test-metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                .andExpect(jsonPath("$.fileName").value("test.txt"))
                .andExpect(jsonPath("$.size").exists());

        System.out.println("✅ Тест успешной загрузки документа пройден");
    }

    @Test
    void testUploadDocument_WithNullMetadata() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Test content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/documents")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                .andExpect(jsonPath("$.fileName").value("test.txt"))
                .andExpect(jsonPath("$.size").exists());

        System.out.println("✅ Тест загрузки с null метаданными пройден");
    }

    @Test
    void testUploadDocument_WithEmptyFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/documents")
                        .file(emptyFile)
                        .param("metadata", "test-metadata"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Файл не выбран или пустой"));

        System.out.println("✅ Тест пустого файла пройден");
    }

    @Test
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
        mockMvc.perform(multipart("/api/documents")
                        .file(largeFile)
                        .param("metadata", "test-metadata"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Размер файла превышает 10MB"));

        System.out.println("✅ Тест превышения размера файла пройден");
    }

    @Test
    void testUploadDocument_WhenIngestionFails() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Test content".getBytes()
        );

        doThrow(new DocumentIngestionException("Document already exists"))
                .when(ingestionService).ingestDocument(any(), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("metadata", "test-metadata"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Document already exists"));

        System.out.println("✅ Тест ошибки загрузки документа пройден");
    }

    @Test
    void testUploadDocument_WhenGeneralExceptionOccurs() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Test content".getBytes()
        );

        doThrow(new RuntimeException("Something went wrong"))
                .when(ingestionService).ingestDocument(any(), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("metadata", "test-metadata"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Внутренняя ошибка сервера: Something went wrong"));

        System.out.println("✅ Тест общей ошибки загрузки документа пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ deleteDocument
    // ============================================================

    @Test
    void testDeleteDocument_Success() throws Exception {
        // Arrange
        Long documentId = 1L;
        when(ingestionService.deleteDocument(documentId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document deleted successfully"))
                .andExpect(jsonPath("$.id").value(documentId));

        System.out.println("✅ Тест успешного удаления документа пройден");
    }

    @Test
    void testDeleteDocument_NotFound() throws Exception {
        // Arrange
        Long documentId = 999L;
        when(ingestionService.deleteDocument(documentId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Документ с ID " + documentId + " не найден"));

        System.out.println("✅ Тест удаления несуществующего документа пройден");
    }

    @Test
    void testDeleteDocument_WithRuntimeException() throws Exception {
        // Arrange
        Long documentId = 1L;
        when(ingestionService.deleteDocument(documentId))
                .thenThrow(new RuntimeException("Document not found"));

        // Act & Assert
        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Документ с ID " + documentId + " не найден"));

        System.out.println("✅ Тест удаления документа с RuntimeException пройден");
    }

    @Test
    void testDeleteDocument_WithGeneralException() throws Exception {
        // Arrange
        Long documentId = 1L;
        when(ingestionService.deleteDocument(documentId))
                .thenThrow(new IllegalStateException("Database error"));

        // Act & Assert
        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Ошибка удаления: Database error"));

        System.out.println("✅ Тест удаления документа с общей ошибкой пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ documentExists
    // ============================================================

    @Test
    void testDocumentExists_WhenExists() throws Exception {
        // Arrange
        String fileName = "test.txt";
        when(ingestionService.documentExists(fileName)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/documents/exists")
                        .param("fileName", fileName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value(fileName))
                .andExpect(jsonPath("$.exists").value(true));

        System.out.println("✅ Тест проверки существования документа (true) пройден");
    }

    @Test
    void testDocumentExists_WhenNotExists() throws Exception {
        // Arrange
        String fileName = "nonexistent.txt";
        when(ingestionService.documentExists(fileName)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/documents/exists")
                        .param("fileName", fileName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value(fileName))
                .andExpect(jsonPath("$.exists").value(false));

        System.out.println("✅ Тест проверки существования документа (false) пройден");
    }

    @Test
    void testDocumentExists_WithException() throws Exception {
        // Arrange
        String fileName = "test.txt";
        when(ingestionService.documentExists(fileName))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/documents/exists")
                        .param("fileName", fileName))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Ошибка проверки: Database error"));

        System.out.println("✅ Тест проверки существования документа с ошибкой пройден");
    }
}