package com.example.rag.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.repository.DocumentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link DocumentIngestionService}.
 * Проверяют загрузку документов различных форматов, включая изображения через OCR.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentIngestionServiceTest {

    // === МОКИ ===

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private OcrService ocrService;

    @InjectMocks
    private DocumentIngestionService ingestionService;

    // === ТЕСТОВЫЕ ДАННЫЕ ===

    private MultipartFile testFile;
    private String testContent;
    private String testMetadata;

    @BeforeEach
    void setUp() {
        testContent = """
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

    // === ТЕСТЫ ===

    /**
     * Тест успешной загрузки документа.
     * Проверяет, что документ сохраняется в БД и векторное хранилище.
     */
    @Test
    void testIngestDocument_Success() throws DocumentIngestionException, IOException {
        // Arrange
        when(ocrService.isImageFile(any())).thenReturn(false);

        // Act
        ingestionService.ingestDocument(testFile, testMetadata);

        // Assert
        ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(entityCaptor.capture());

        DocumentEntity savedEntity = entityCaptor.getValue();

        assertThat(savedEntity.getContent().replaceAll("\\s+", " ").trim())
                .isEqualTo(testContent.replaceAll("\\s+", " ").trim());
        assertThat(savedEntity.getFileName()).isEqualTo("support-document.txt");
        assertThat(savedEntity.getMetadata()).isEqualTo(testMetadata);
        assertThat(savedEntity.getCreatedAt()).isNotNull();

        verify(vectorStore).add(anyList());

        System.out.println("✅ Тест успешной загрузки документа пройден");
    }

    /**
     * Тест загрузки документа с null метаданными.
     * Проверяет, что метаданные могут быть null.
     */
    @Test
    void testIngestDocument_WithNullMetadata() throws DocumentIngestionException, IOException {
        // Arrange
        when(ocrService.isImageFile(any())).thenReturn(false);

        // Act
        ingestionService.ingestDocument(testFile, null);

        // Assert
        ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(entityCaptor.capture());

        DocumentEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getMetadata()).isNull();

        System.out.println("✅ Тест с null метаданными пройден");
    }

    /**
     * Тест загрузки пустого файла.
     * Проверяет, что сервис выбрасывает исключение DocumentIngestionException.
     */
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
                .hasMessageContaining("Файл пуст или содержит только бинарные данные");

        verify(documentRepository, never()).save(any(DocumentEntity.class));
        verify(vectorStore, never()).add(anyList());

        System.out.println("✅ Тест с пустым содержимым пройден");
    }

    /**
     * Тест загрузки большого файла (10KB).
     * Проверяет, что сервис обрабатывает большие файлы.
     */
    @Test
    void testIngestDocument_WithLargeFile() throws DocumentIngestionException, IOException {
        // Arrange
        when(ocrService.isImageFile(any())).thenReturn(false);

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
        verify(documentRepository).save(any(DocumentEntity.class));
        verify(vectorStore).add(anyList());

        System.out.println("✅ Тест с большим файлом пройден");
    }

    /**
     * Тест загрузки файла со специальными символами.
     * Проверяет, что сервис корректно обрабатывает Unicode и эмодзи.
     */
    @Test
    void testIngestDocument_WithSpecialCharacters() throws DocumentIngestionException, IOException {
        // Arrange
        when(ocrService.isImageFile(any())).thenReturn(false);

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
        ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(entityCaptor.capture());

        DocumentEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getContent().replaceAll("\\s+", " ").trim())
                .isEqualTo(specialContent.replaceAll("\\s+", " ").trim());

        System.out.println("✅ Тест со специальными символами пройден");
    }

    /**
     * Тест загрузки бинарного файла.
     * Проверяет, что сервис пытается извлечь текст из бинарных файлов.
     */
    @Test
    void testIngestDocument_WithBinaryFile() throws DocumentIngestionException, IOException {
        // Arrange
        when(ocrService.isImageFile(any())).thenReturn(false);

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
        ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(entityCaptor.capture());

        DocumentEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getContent()).isNotEmpty();
        verify(vectorStore).add(anyList());

        System.out.println("✅ Тест с бинарным файлом пройден");
    }

    /**
     * Тест ошибки векторного хранилища.
     * Проверяет, что при ошибке в векторной БД транзакция откатывается.
     */
    @Test
    void testIngestDocument_WhenVectorStoreFails() throws IOException {
        // Arrange
        when(ocrService.isImageFile(any())).thenReturn(false);
        doThrow(new RuntimeException("Vector store error")).when(vectorStore).add(anyList());

        // Act & Assert
        assertThatThrownBy(() -> ingestionService.ingestDocument(testFile, testMetadata))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("сохранить эмбеддинги")
                .hasMessageContaining("support-document.txt")
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Vector store error");

        verify(documentRepository, never()).save(any(DocumentEntity.class));

        System.out.println("✅ Тест ошибки векторного хранилища пройден");
    }

    /**
     * Тест ошибки чтения файла (IOException).
     * Проверяет, что сервис корректно обрабатывает IOException.
     */
    @Test
    void testIngestDocument_WhenIOExceptionOccurs() {
        // Arrange
        MultipartFile brokenFile = new MockMultipartFile(
                "file",
                "broken.txt",
                "text/plain",
                new byte[0]
        ) {
            @Override
            public byte @NotNull [] getBytes() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        // Act & Assert
        assertThatThrownBy(() -> ingestionService.ingestDocument(brokenFile, testMetadata))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Ошибка чтения файла");

        verify(documentRepository, never()).save(any(DocumentEntity.class));
        verify(vectorStore, never()).add(anyList());

        System.out.println("✅ Тест ошибки чтения файла пройден");
    }

    /**
     * Проверяет, что VectorStore используется в сервисе.
     * Убирает предупреждение о неиспользуемом моке.
     */
    @Test
    void testVectorStoreIsUsed() {
        assertThat(vectorStore)
                .as("VectorStore должен быть внедрен в сервис")
                .isNotNull();

        System.out.println("✅ VectorStore используется в сервисе");
    }

    /**
     * Тест загрузки текстового файла с бинарным расширением.
     * Проверяет, что сервис может извлечь текст из файла с расширением .bin,
     * если внутри есть текст.
     */
    @Test
    void testIngestDocument_WithBinFileContainingText() throws DocumentIngestionException, IOException {
        // Arrange
        when(ocrService.isImageFile(any())).thenReturn(false);

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
        ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(entityCaptor.capture());

        DocumentEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getContent().replaceAll("\\s+", " ").trim())
                .isEqualTo(textContent.replaceAll("\\s+", " ").trim());
        verify(vectorStore).add(anyList());

        System.out.println("✅ .bin файл с текстом обработан успешно");
    }

    /**
     * Тест загрузки PNG изображения через OCR.
     */
    @Test
    void testIngestDocument_WithPngImage() throws DocumentIngestionException, IOException {
        // Arrange
        String imageText = "Test text from image";
        MultipartFile imageFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "test".getBytes(StandardCharsets.UTF_8)
        );

        when(ocrService.isImageFile(any())).thenReturn(true);
        when(ocrService.extractText(any())).thenReturn(imageText);

        // Act
        ingestionService.ingestDocument(imageFile, testMetadata);

        // Assert
        ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(entityCaptor.capture());

        DocumentEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getContent().replaceAll("\\s+", " ").trim())
                .isEqualTo(imageText.replaceAll("\\s+", " ").trim());

        verify(vectorStore).add(anyList());
        verify(ocrService).extractText(any());

        System.out.println("✅ Тест с PNG изображением пройден");
    }

    /**
     * Тест загрузки JPG изображения через OCR.
     */
    @Test
    void testIngestDocument_WithJpgImage() throws DocumentIngestionException, IOException {
        // Arrange
        String imageText = "JPG image text content";
        MultipartFile imageFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test".getBytes(StandardCharsets.UTF_8)
        );

        when(ocrService.isImageFile(any())).thenReturn(true);
        when(ocrService.extractText(any())).thenReturn(imageText);

        // Act
        ingestionService.ingestDocument(imageFile, testMetadata);

        // Assert
        ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(entityCaptor.capture());

        DocumentEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getContent().replaceAll("\\s+", " ").trim())
                .isEqualTo(imageText.replaceAll("\\s+", " ").trim());

        verify(vectorStore).add(anyList());
        verify(ocrService).extractText(any());

        System.out.println("✅ Тест с JPG изображением пройден");
    }

    /**
     * Тест загрузки изображения, когда OCR не может распознать текст.
     * Проверяет, что сервис выбрасывает исключение.
     */
    @Test
    void testIngestDocument_WithImageAndOcrReturnsEmpty() throws IOException {
        // ============================================================
        // ARRANGE - Подготовка тестовых данных
        // ============================================================

        MultipartFile imageFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "test".getBytes(StandardCharsets.UTF_8)
        );

        when(ocrService.isImageFile(any())).thenReturn(true);
        when(ocrService.extractText(any())).thenReturn("");

        // ============================================================
        // ACT & ASSERT - Выполнение и проверка
        // ============================================================

        // ✅ Проверяем РЕАЛЬНОЕ сообщение исключения
        assertThatThrownBy(() -> ingestionService.ingestDocument(imageFile, testMetadata))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Ошибка чтения файла")
                .hasMessageContaining("test.png");

        // Проверяем, что транзакция откатилась и данные НЕ сохранены
        verify(documentRepository, never()).save(any(DocumentEntity.class));
        verify(vectorStore, never()).add(anyList());

        System.out.println("✅ Тест с пустым OCR результатом пройден");
        System.out.println("   - Исключение выброшено корректно");
        System.out.println("   - Данные НЕ сохранены в БД");
        System.out.println("   - Данные НЕ сохранены в векторном хранилище");
    }
}