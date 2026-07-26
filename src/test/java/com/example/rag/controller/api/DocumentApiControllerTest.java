package com.example.rag.controller.api;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.service.DocumentIngestionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link DocumentApiController}.
 * Проверяют REST API эндпоинты для работы с документами.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class DocumentApiControllerTest {

    // === МОКИ ===

    @Mock
    private DocumentIngestionService ingestionService;

    @InjectMocks
    private DocumentApiController controller;

    // === ТЕСТОВЫЕ ДАННЫЕ ===

    private MultipartFile testFile;
    private String testFileName;
    private String testMetadata;
    private String testContent;

    @BeforeEach
    void setUp() {
        testFileName = "test-document.txt";
        testMetadata = "{\"author\":\"test\"}";
        testContent = "Test document content for API tests.";
        testFile = new MockMultipartFile(
                "file",
                testFileName,
                "text/plain",
                testContent.getBytes()
        );
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ uploadDocument
    // ============================================================

    /**
     * Тест успешной загрузки документа.
     */
    @Test
    void testUploadDocument_Success() {
        // Arrange
        when(ingestionService.documentExists(testFileName)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.uploadDocument(testFile, testMetadata);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("message", "Document uploaded successfully")
                .containsEntry("fileName", testFileName)
                .containsKey("size");

        verify(ingestionService).ingestDocument(testFile, testMetadata);

        System.out.println("✅ Тест успешной загрузки документа пройден");
    }

    /**
     * Тест загрузки документа с null метаданными.
     */
    @Test
    void testUploadDocument_WithNullMetadata() {
        // Arrange
        when(ingestionService.documentExists(testFileName)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.uploadDocument(testFile, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("message", "Document uploaded successfully")
                .containsEntry("fileName", testFileName);

        verify(ingestionService).ingestDocument(testFile, null);

        System.out.println("✅ Тест загрузки с null метаданными пройден");
    }

    /**
     * Тест загрузки пустого файла.
     */
    @Test
    void testUploadDocument_WithEmptyFile() {
        // Arrange
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // Act
        ResponseEntity<Map<String, Object>> response = controller.uploadDocument(emptyFile, testMetadata);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("error", "Файл не выбран или пустой");

        verify(ingestionService, never()).ingestDocument(any(), any());

        System.out.println("✅ Тест пустого файла пройден");
    }

    /**
     * Тест загрузки файла с превышением размера.
     */
    @Test
    void testUploadDocument_WithFileTooLarge() {
        // Arrange
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent
        );

        // Act
        ResponseEntity<Map<String, Object>> response = controller.uploadDocument(largeFile, testMetadata);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("error", "Размер файла превышает 10MB");

        verify(ingestionService, never()).ingestDocument(any(), any());

        System.out.println("✅ Тест превышения размера файла пройден");
    }

    /**
     * Тест загрузки документа с ошибкой DocumentIngestionException.
     */
    @Test
    void testUploadDocument_WhenIngestionFails() throws Exception {
        // Arrange
        String errorMessage = "Document already exists";
        doThrow(new DocumentIngestionException(errorMessage))
                .when(ingestionService).ingestDocument(any(), any());

        // Act
        ResponseEntity<Map<String, Object>> response = controller.uploadDocument(testFile, testMetadata);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("error", errorMessage);

        System.out.println("✅ Тест ошибки загрузки документа пройден");
    }

    /**
     * Тест загрузки документа с общей ошибкой.
     */
    @Test
    void testUploadDocument_WhenGeneralExceptionOccurs() throws Exception {
        // Arrange
        String errorMessage = "Something went wrong";
        doThrow(new RuntimeException(errorMessage))
                .when(ingestionService).ingestDocument(any(), any());

        // Act
        ResponseEntity<Map<String, Object>> response = controller.uploadDocument(testFile, testMetadata);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("error", "Внутренняя ошибка сервера: " + errorMessage);

        System.out.println("✅ Тест общей ошибки загрузки документа пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ deleteDocument
    // ============================================================

    /**
     * Тест успешного удаления документа.
     */
    @Test
    void testDeleteDocument_Success() {
        // Arrange
        Long documentId = 1L;
        when(ingestionService.deleteDocument(documentId)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.deleteDocument(documentId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("message", "Document deleted successfully")
                .containsEntry("id", documentId);

        verify(ingestionService).deleteDocument(documentId);

        System.out.println("✅ Тест успешного удаления документа пройден");
    }

    /**
     * Тест удаления несуществующего документа.
     */
    @Test
    void testDeleteDocument_NotFound() {
        // Arrange
        Long documentId = 999L;
        when(ingestionService.deleteDocument(documentId)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.deleteDocument(documentId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("error", "Документ с ID " + documentId + " не найден");

        System.out.println("✅ Тест удаления несуществующего документа пройден");
    }

    /**
     * Тест удаления документа с исключением RuntimeException.
     */
    @Test
    void testDeleteDocument_WithRuntimeException() {
        // Arrange
        Long documentId = 1L;
        when(ingestionService.deleteDocument(documentId))
                .thenThrow(new RuntimeException("Document not found"));

        // Act
        ResponseEntity<Map<String, Object>> response = controller.deleteDocument(documentId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("error", "Документ с ID " + documentId + " не найден");

        System.out.println("✅ Тест удаления документа с RuntimeException пройден");
    }

    /**
     * Тест удаления документа с общей ошибкой.
     */
    @Test
    void testDeleteDocument_WithGeneralException() {
        // Arrange
        Long documentId = 1L;
        when(ingestionService.deleteDocument(documentId))
                .thenThrow(new IllegalStateException("Database error"));

        // Act
        ResponseEntity<Map<String, Object>> response = controller.deleteDocument(documentId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isNotNull()
                .containsKey("error");

        System.out.println("✅ Тест удаления документа с общей ошибкой пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ documentExists
    // ============================================================

    /**
     * Тест проверки существования документа (существует).
     */
    @Test
    void testDocumentExists_WhenExists() {
        // Arrange
        String fileName = "test.txt";
        when(ingestionService.documentExists(fileName)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.documentExists(fileName);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("fileName", fileName)
                .containsEntry("exists", true);

        System.out.println("✅ Тест проверки существования документа (true) пройден");
    }

    /**
     * Тест проверки существования документа (не существует).
     */
    @Test
    void testDocumentExists_WhenNotExists() {
        // Arrange
        String fileName = "nonexistent.txt";
        when(ingestionService.documentExists(fileName)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.documentExists(fileName);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("fileName", fileName)
                .containsEntry("exists", false);

        System.out.println("✅ Тест проверки существования документа (false) пройден");
    }

    /**
     * Тест проверки существования документа с ошибкой.
     */
    @Test
    void testDocumentExists_WithException() {
        // Arrange
        String fileName = "test.txt";
        when(ingestionService.documentExists(fileName))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<Map<String, Object>> response = controller.documentExists(fileName);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isNotNull()
                .containsKey("error");

        System.out.println("✅ Тест проверки существования документа с ошибкой пройден");
    }
}