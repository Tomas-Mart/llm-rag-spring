package com.example.rag.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.service.DocumentService;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты для {@link DocumentService}.
 * Проверяют загрузку документов с реальной БД и VectorStore.
 *
 * <p>Тестируемые сценарии:
 * <ul>
 *   <li>Загрузка одного документа</li>
 *   <li>Загрузка нескольких документов</li>
 *   <li>Загрузка пустого документа</li>
 *   <li>Загрузка большого документа</li>
 *   <li>Загрузка документа из реального файла</li>
 *   <li>Предотвращение дубликатов</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 6.0
 * @see DocumentService
 * @see BaseIntegrationTestWithContainers
 * @since 1.0
 */
@Slf4j
@Epic("Интеграционные тесты")
@Feature("Загрузка документов")
class DocumentServiceIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String TEST_FILE_NAME = "test-document.txt";
    private static final String METADATA = "{\"author\":\"integration-test\"}";
    private static final int LARGE_DOCUMENT_REPEAT = 2500;

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @TempDir
    private Path tempDir;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        log.info("🧹 Repository cleared");
    }

    // ============================================================
    // ТЕСТЫ - ЗАГРУЗКА ДОКУМЕНТОВ
    // ============================================================

    @Test
    @Description("Проверка загрузки одного документа")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.CRITICAL)
    void shouldIngestSingleDocument() {
        logTestStart("Testing single document ingestion");

        // Given
        String content = """
                This is an integration test document.
                It should be processed by the real VectorStore and Database.
                Spring AI makes RAG applications easy.
                """;
        MultipartFile file = createMockFile(TEST_FILE_NAME, content);

        // When
        documentService.ingestDocument(file, METADATA);

        // Then
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);

        DocumentEntity saved = documents.getFirst();
        assertThat(saved.getFileName()).isEqualTo(TEST_FILE_NAME);
        assertThat(saved.getContent().trim()).isEqualTo(content.trim());
        assertThat(saved.getMetadata()).isEqualTo(METADATA);
        assertThat(saved.getCreatedAt()).isNotNull();

        log.info("✅ Document saved with ID: {}", saved.getId());
        logTestSuccess("Single document ingestion completed");
    }

    @Test
    @Description("Проверка загрузки нескольких документов")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    void shouldIngestMultipleDocuments() {
        logTestStart("Testing multiple documents ingestion");

        // Given
        String[] contents = {
                "First document for integration testing.",
                "Second document for integration testing.",
                "Third document for integration testing."
        };

        // When
        for (int i = 0; i < contents.length; i++) {
            MultipartFile file = createMockFile("doc-" + i + ".txt", contents[i]);
            documentService.ingestDocument(file, "batch-" + i);
        }

        // Then
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(contents.length);

        for (int i = 0; i < contents.length; i++) {
            DocumentEntity doc = documents.get(i);
            assertThat(doc.getFileName()).isEqualTo("doc-" + i + ".txt");
            assertThat(doc.getContent().trim()).isEqualTo(contents[i].trim());
            assertThat(doc.getMetadata()).isEqualTo("batch-" + i);
        }

        log.info("✅ Loaded {} documents", documents.size());
        logTestSuccess("Multiple documents ingestion completed");
    }

    @Test
    @Description("Проверка загрузки пустого документа")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.NORMAL)
    void shouldRejectEmptyDocument() {
        logTestStart("Testing empty document rejection");

        // Given
        MultipartFile emptyFile = createMockFile("empty.txt", "");

        // When & Then
        assertThatThrownBy(() -> documentService.ingestDocument(emptyFile, METADATA))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Файл пуст");

        assertThat(documentRepository.findAll()).isEmpty();
        logTestSuccess("Empty document correctly rejected");
    }

    @Test
    @Description("Проверка загрузки большого документа")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    void shouldIngestLargeDocument() {
        logTestStart("Testing large document ingestion");

        // Given
        String largeContent = "Large integration test content. ".repeat(LARGE_DOCUMENT_REPEAT);
        MultipartFile file = createMockFile("large.txt", largeContent);

        // When
        documentService.ingestDocument(file, "large-document");

        // Then
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);

        DocumentEntity saved = documents.getFirst();
        assertThat(saved.getContent().trim().length())
                .isEqualTo(largeContent.trim().length());

        log.info("✅ Large document saved ({} KB)", saved.getContent().length() / 1024);
        logTestSuccess("Large document ingestion completed");
    }

    // ============================================================
    // ТЕСТЫ - ЗАГРУЗКА ИЗ РЕАЛЬНЫХ ФАЙЛОВ
    // ============================================================

    @Test
    @Description("Проверка загрузки документа из реального файла")
    @Story("Работа с файловой системой")
    @Severity(SeverityLevel.NORMAL)
    void shouldIngestDocumentFromRealFile() throws IOException {
        logTestStart("Testing real file ingestion");

        // Given
        String content = "This is a real file content for testing.";
        Path realFile = tempDir.resolve("real-file.txt");
        Files.writeString(realFile, content);

        MultipartFile file = new MockMultipartFile(
                "file",
                realFile.getFileName().toString(),
                "text/plain",
                Files.readAllBytes(realFile)
        );

        // When
        documentService.ingestDocument(file, "real-file");

        // Then
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);

        DocumentEntity saved = documents.getFirst();
        assertThat(saved.getFileName()).isEqualTo("real-file.txt");
        assertThat(saved.getContent().trim()).isEqualTo(content.trim());

        log.info("✅ Real file ingested with ID: {}", saved.getId());
        logTestSuccess("Real file ingestion completed");
    }

    @Test
    @Description("Проверка предотвращения дубликатов файлов")
    @Story("Работа с файловой системой")
    @Severity(SeverityLevel.NORMAL)
    void shouldPreventDuplicateFileIngestion() throws IOException {
        logTestStart("Testing duplicate file prevention");

        // Given
        String content = "Test file for duplicate checking.";
        Path file = tempDir.resolve("duplicate.txt");
        Files.writeString(file, content);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                file.getFileName().toString(),
                "text/plain",
                Files.readAllBytes(file)
        );

        // When - первый раз успешно
        documentService.ingestDocument(multipartFile, "first");

        // Then - второй раз должно выбросить исключение
        assertThatThrownBy(() -> documentService.ingestDocument(multipartFile, "second"))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("уже существует");

        assertThat(documentRepository.findAll()).hasSize(1);
        logTestSuccess("Duplicate file correctly rejected");
    }

    // ============================================================
    // ТЕСТЫ - КРАЕВЫЕ СЛУЧАИ
    // ============================================================

    @Test
    @Description("Проверка загрузки документа с null именем файла")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.NORMAL)
    void shouldRejectNullFileName() {
        logTestStart("Testing null file name rejection");

        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                null,
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThatThrownBy(() -> documentService.ingestDocument(file, METADATA))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Имя файла отсутствует");

        assertThat(documentRepository.findAll()).isEmpty();
        logTestSuccess("Null file name correctly rejected");
    }

    @Test
    @Description("Проверка загрузки документа с превышением размера")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.NORMAL)
    void shouldRejectFileTooLarge() {
        logTestStart("Testing file size limit");

        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                largeContent
        );

        // When & Then
        assertThatThrownBy(() -> documentService.ingestDocument(file, METADATA))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("превышает");

        assertThat(documentRepository.findAll()).isEmpty();
        logTestSuccess("File too large correctly rejected");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Создает MockMultipartFile для тестирования.
     *
     * @param fileName имя файла
     * @param content  содержимое
     * @return MockMultipartFile
     */
    private MultipartFile createMockFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}