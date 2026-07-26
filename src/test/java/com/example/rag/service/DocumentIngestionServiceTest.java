package com.example.rag.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты для {@link DocumentIngestionService}.
 * Проверяют загрузку документов с реальной БД и VectorStore.
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Успешная загрузка документа</li>
 *   <li>Загрузка с null метаданными</li>
 *   <li>Загрузка пустого файла</li>
 *   <li>Загрузка большого файла</li>
 *   <li>Загрузка со специальными символами</li>
 *   <li>Загрузка бинарного файла</li>
 *   <li>Загрузка PNG изображения через OCR</li>
 *   <li>Загрузка JPG изображения через OCR</li>
 *   <li>Загрузка с пустым OCR результатом</li>
 *   <li>Перезагрузка документа</li>
 *   <li>Удаление документа по ID</li>
 *   <li>Удаление несуществующего документа</li>
 *   <li>Удаление документа по имени файла</li>
 *   <li>Удаление несуществующего документа по имени</li>
 *   <li>Проверка существования документа</li>
 *   <li>Получение документа по ID</li>
 *   <li>Получение несуществующего документа</li>
 *   <li>Получение документа по имени файла</li>
 *   <li>Получение несуществующего документа по имени</li>
 *   <li>Получение всех документов</li>
 *   <li>Получение всех документов (пустой список)</li>
 *   <li>Очистка всех документов</li>
 *   <li>Загрузка дубликата документа</li>
 *   <li>Загрузка файла с превышением размера</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 2.0
 * @see DocumentIngestionService
 * @see BaseIntegrationTestWithContainers
 * @since 1.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class DocumentIngestionServiceTest extends BaseIntegrationTestWithContainers {

    @Autowired
    private DocumentIngestionService ingestionService;

    @Autowired
    private DocumentRepository documentRepository;

    private MultipartFile testFile;
    private String testMetadata;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();

        String testContent = """
                Spring AI is a framework for building AI applications with Spring Boot.
                It provides integration with various LLM providers and vector databases.
                This is a support document for RAG application.
                """;

        testMetadata = "{\"author\":\"support\",\"category\":\"documentation\"}";

        testFile = new MockMultipartFile(
                "file",
                "support-document.txt",
                "text/plain",
                testContent.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ ingestDocument
    // ============================================================

    @Test
    void testIngestDocument_Success() throws DocumentIngestionException {
        // Act
        ingestionService.ingestDocument(testFile, testMetadata);

        // Assert
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);

        DocumentEntity savedEntity = documents.getFirst();
        assertThat(savedEntity.getFileName()).isEqualTo("support-document.txt");
        assertThat(savedEntity.getContent()).contains("Spring AI");
        assertThat(savedEntity.getMetadata()).isEqualTo(testMetadata);
        assertThat(savedEntity.getCreatedAt()).isNotNull();

        log.info("✅ Тест успешной загрузки документа пройден");
    }

    @Test
    void testIngestDocument_WithNullMetadata() throws DocumentIngestionException {
        // Act
        ingestionService.ingestDocument(testFile, null);

        // Assert
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);

        DocumentEntity savedEntity = documents.getFirst();
        assertThat(savedEntity.getMetadata()).isNull();

        log.info("✅ Тест с null метаданными пройден");
    }

    @Test
    void testIngestDocument_WithEmptyContent() {
        // Arrange
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                "".getBytes(StandardCharsets.UTF_8)
        );

        // Act & Assert
        assertThatThrownBy(() -> ingestionService.ingestDocument(emptyFile, testMetadata))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Файл пуст");

        assertThat(documentRepository.findAll()).isEmpty();

        log.info("✅ Тест с пустым содержимым пройден");
    }

    @Test
    void testIngestDocument_WithLargeFile() throws DocumentIngestionException {
        // Arrange
        String largeContent = "A".repeat(10000);
        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ingestionService.ingestDocument(largeFile, testMetadata);

        // Assert
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);
        // ✅ Исправлено: используем trim() для игнорирования лишних пробелов/переводов строк
        assertThat(documents.getFirst().getContent().trim().length()).isEqualTo(10000);

        log.info("✅ Тест с большим файлом пройден");
    }

    @Test
    void testIngestDocument_WithSpecialCharacters() throws DocumentIngestionException {
        // Arrange
        String specialContent = """
                Специальные символы: !@#$%^&*()_+{}|:"<>?
                Unicode: 中文, 日本語, 한국어, 🚀🎉
                Тест с кириллицей и эмодзи 😊
                """;

        MultipartFile specialFile = new MockMultipartFile(
                "file",
                "special.txt",
                "text/plain",
                specialContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ingestionService.ingestDocument(specialFile, testMetadata);

        // Assert
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);

        DocumentEntity savedEntity = documents.getFirst();
        assertThat(savedEntity.getContent()).contains("Специальные символы");
        assertThat(savedEntity.getContent()).contains("🚀🎉");
        assertThat(savedEntity.getContent()).contains("кириллицей");

        log.info("✅ Тест со специальными символами пройден");
    }

    @Test
    void testIngestDocument_WithBinaryFile() throws DocumentIngestionException {
        // Arrange
        String contentWithBinary = "PDF header with actual text content: Hello World! This is a test document.";
        byte[] binaryData = contentWithBinary.getBytes(StandardCharsets.UTF_8);

        MultipartFile binaryFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                binaryData
        );

        // Act
        ingestionService.ingestDocument(binaryFile, testMetadata);

        // Assert
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getContent()).isNotEmpty();

        log.info("✅ Тест с бинарным файлом пройден");
    }

    @Test
    void testIngestDocument_WithBinFileContainingText() throws DocumentIngestionException {
        // Arrange
        String textContent = "This is a text file disguised as .bin file with some content inside.";
        MultipartFile binFile = new MockMultipartFile(
                "file",
                "data.bin",
                "application/octet-stream",
                textContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ingestionService.ingestDocument(binFile, testMetadata);

        // Assert
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);

        DocumentEntity savedEntity = documents.getFirst();
        assertThat(savedEntity.getContent()).contains("text file disguised");

        log.info("✅ .bin файл с текстом обработан успешно");
    }

    @Test
    void testIngestDocument_WithPngImage() {
        // Arrange
        byte[] pngData = new byte[]{
                (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D, (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52
        };
        MultipartFile pngFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                pngData
        );

        // Act & Assert - ожидаем исключение для невалидных данных
        assertThatThrownBy(() -> ingestionService.ingestDocument(pngFile, testMetadata))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Ошибка чтения файла");

        assertThat(documentRepository.findAll()).isEmpty();
        log.info("✅ Тест с PNG изображением: ожидаемое исключение получено");
    }

    @Test
    void testIngestDocument_WithJpgImage() {
        // Arrange
        byte[] jpgData = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, (byte) 0x00, (byte) 0x10, (byte) 0x4A, (byte) 0x46,
                (byte) 0x49, (byte) 0x46, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x48
        };
        MultipartFile jpgFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                jpgData
        );

        // Act & Assert - ожидаем исключение для невалидных данных
        assertThatThrownBy(() -> ingestionService.ingestDocument(jpgFile, testMetadata))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Ошибка чтения файла");

        assertThat(documentRepository.findAll()).isEmpty();
        log.info("✅ Тест с JPG изображением: ожидаемое исключение получено");
    }

    @Test
    void testIngestDocument_WithImageAndOcrReturnsEmpty() {
        // Arrange
        byte[] imageData = "fake image data".getBytes(StandardCharsets.UTF_8);
        MultipartFile imageFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                imageData
        );

        // Act & Assert
        assertThatThrownBy(() -> ingestionService.ingestDocument(imageFile, testMetadata))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Ошибка");

        assertThat(documentRepository.findAll()).isEmpty();

        log.info("✅ Тест с пустым OCR результатом пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ reIngestDocument
    // ============================================================

    @Test
    void testReIngestDocument_Success() throws DocumentIngestionException {
        // Arrange
        ingestionService.ingestDocument(testFile, testMetadata);
        assertThat(documentRepository.findAll()).hasSize(1);

        // Act
        ingestionService.reIngestDocument(testFile, testMetadata);

        // Assert
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getFileName()).isEqualTo("support-document.txt");

        log.info("✅ Тест перезагрузки документа пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ deleteDocument
    // ============================================================

    @Test
    void testDeleteDocument_Success() throws DocumentIngestionException {
        // Arrange
        ingestionService.ingestDocument(testFile, testMetadata);
        List<DocumentEntity> documents = documentRepository.findAll();
        Long documentId = documents.getFirst().getId();

        // Act
        boolean result = ingestionService.deleteDocument(documentId);

        // Assert
        assertThat(result).isTrue();
        assertThat(documentRepository.findById(documentId)).isEmpty();

        log.info("✅ Тест удаления документа по ID пройден");
    }

    @Test
    void testDeleteDocument_NotFound() {
        // Act
        boolean result = ingestionService.deleteDocument(999L);

        // Assert
        assertThat(result).isFalse();

        log.info("✅ Тест удаления несуществующего документа пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ deleteDocumentByFileName
    // ============================================================

    @Test
    void testDeleteDocumentByFileName_Success() throws DocumentIngestionException {
        // Arrange
        ingestionService.ingestDocument(testFile, testMetadata);
        assertThat(documentRepository.findAll()).hasSize(1);

        // Act
        boolean result = ingestionService.deleteDocumentByFileName("support-document.txt");

        // Assert
        assertThat(result).isTrue();
        assertThat(documentRepository.findAll()).isEmpty();

        log.info("✅ Тест удаления документа по имени файла пройден");
    }

    @Test
    void testDeleteDocumentByFileName_NotFound() {
        // Act
        boolean result = ingestionService.deleteDocumentByFileName("nonexistent.txt");

        // Assert
        assertThat(result).isFalse();

        log.info("✅ Тест удаления несуществующего документа по имени пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ documentExists
    // ============================================================

    @Test
    void testDocumentExists_WhenExists() throws DocumentIngestionException {
        // Arrange
        ingestionService.ingestDocument(testFile, testMetadata);

        // Act
        boolean exists = ingestionService.documentExists("support-document.txt");

        // Assert
        assertThat(exists).isTrue();

        log.info("✅ Тест проверки существования документа (true) пройден");
    }

    @Test
    void testDocumentExists_WhenNotExists() {
        // Act
        boolean exists = ingestionService.documentExists("nonexistent.txt");

        // Assert
        assertThat(exists).isFalse();

        log.info("✅ Тест проверки существования документа (false) пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ getDocument
    // ============================================================

    @Test
    void testGetDocument_Success() throws DocumentIngestionException {
        // Arrange
        ingestionService.ingestDocument(testFile, testMetadata);
        List<DocumentEntity> documents = documentRepository.findAll();
        Long documentId = documents.getFirst().getId();

        // Act
        Optional<DocumentEntity> result = ingestionService.getDocument(documentId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getFileName()).isEqualTo("support-document.txt");

        log.info("✅ Тест получения документа по ID пройден");
    }

    @Test
    void testGetDocument_NotFound() {
        // Act
        Optional<DocumentEntity> result = ingestionService.getDocument(999L);

        // Assert
        assertThat(result).isEmpty();

        log.info("✅ Тест получения несуществующего документа пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ getDocumentByFileName
    // ============================================================

    @Test
    void testGetDocumentByFileName_Success() throws DocumentIngestionException {
        // Arrange
        ingestionService.ingestDocument(testFile, testMetadata);

        // Act
        Optional<DocumentEntity> result = ingestionService.getDocumentByFileName("support-document.txt");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getFileName()).isEqualTo("support-document.txt");

        log.info("✅ Тест получения документа по имени файла пройден");
    }

    @Test
    void testGetDocumentByFileName_NotFound() {
        // Act
        Optional<DocumentEntity> result = ingestionService.getDocumentByFileName("nonexistent.txt");

        // Assert
        assertThat(result).isEmpty();

        log.info("✅ Тест получения несуществующего документа по имени пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ getAllDocuments
    // ============================================================

    @Test
    void testGetAllDocuments() throws DocumentIngestionException {
        // Arrange
        ingestionService.ingestDocument(testFile, testMetadata);

        // Act
        List<DocumentEntity> result = ingestionService.getAllDocuments();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getFileName()).isEqualTo("support-document.txt");

        log.info("✅ Тест получения всех документов пройден");
    }

    @Test
    void testGetAllDocuments_EmptyList() {
        // Act
        List<DocumentEntity> result = ingestionService.getAllDocuments();

        // Assert
        assertThat(result).isEmpty();

        log.info("✅ Тест получения всех документов (пустой список) пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ clearAllDocuments
    // ============================================================

    @Test
    void testClearAllDocuments() throws DocumentIngestionException {
        // Arrange
        ingestionService.ingestDocument(testFile, testMetadata);
        assertThat(documentRepository.findAll()).hasSize(1);

        // Act
        ingestionService.clearAllDocuments();

        // Assert
        assertThat(documentRepository.findAll()).isEmpty();

        log.info("✅ Тест очистки всех документов пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ ДУБЛИКАТОВ И РАЗМЕРА
    // ============================================================

    @Test
    void testIngestDocument_WhenDocumentAlreadyExists() throws DocumentIngestionException {
        // Arrange
        ingestionService.ingestDocument(testFile, testMetadata);

        // Act & Assert
        assertThatThrownBy(() -> ingestionService.ingestDocument(testFile, testMetadata))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("уже существует");

        log.info("✅ Тест загрузки дубликата документа пройден");
    }

    @Test
    void testIngestDocument_WhenFileTooLarge() {
        // Arrange
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent
        );

        // Act & Assert
        assertThatThrownBy(() -> ingestionService.ingestDocument(largeFile, testMetadata))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("превышает");

        assertThat(documentRepository.findAll()).isEmpty();

        log.info("✅ Тест превышения размера файла пройден");
    }
}