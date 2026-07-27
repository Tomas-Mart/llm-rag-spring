package com.example.rag.unit.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.service.DocumentService;
import com.example.rag.service.OcrService;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Модульные тесты DocumentService")
class DocumentServiceUnitTest {

    // ============================================================
    // ЗАВИСИМОСТИ (МОКИ)
    // ============================================================

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private OcrService ocrService;

    @Mock
    private MultipartFile multipartFile;

    @Spy
    @InjectMocks
    private DocumentService documentService;

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String TEST_FILE_NAME = "test-document.txt";
    private static final String TEST_CONTENT = "This is a test document content for unit testing.";
    private static final String TEST_METADATA = "{\"author\":\"unit-test\",\"category\":\"testing\"}";
    private static final Long TEST_ID = 1L;
    private static final Long NON_EXISTENT_ID = 999L;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        // Пустой setUp - все моки настраиваются в каждом тесте индивидуально
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ ingestDocument
    // ============================================================

    @Test
    @DisplayName("Успешная загрузка документа")
    void testIngestDocument_Success() throws DocumentIngestionException, IOException {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getBytes()).thenReturn(TEST_CONTENT.getBytes(StandardCharsets.UTF_8));
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(TEST_CONTENT.getBytes(StandardCharsets.UTF_8)));
        when(ocrService.isImageFile(any(MultipartFile.class))).thenReturn(false);
        when(documentRepository.findByFileName(TEST_FILE_NAME)).thenReturn(Optional.empty());

        DocumentEntity savedEntity = DocumentEntity.builder()
                .id(TEST_ID)
                .fileName(TEST_FILE_NAME)
                .content(TEST_CONTENT)
                .metadata(TEST_METADATA)
                .build();

        when(documentRepository.save(any(DocumentEntity.class))).thenReturn(savedEntity);
        doNothing().when(vectorStore).add(anyList());

        // When
        documentService.ingestDocument(multipartFile, TEST_METADATA);

        // Then
        verify(documentRepository).findByFileName(TEST_FILE_NAME);
        verify(documentRepository).save(any(DocumentEntity.class));
        verify(vectorStore).add(anyList());

        log.info("✅ Тест успешной загрузки документа пройден");
    }

    @Test
    @DisplayName("Загрузка документа с превышением размера")
    void testIngestDocument_WhenFileTooLarge() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
        when(multipartFile.getSize()).thenReturn(11L * 1024 * 1024);

        // When & Then
        assertThatThrownBy(() -> documentService.ingestDocument(multipartFile, TEST_METADATA))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("превышает");

        verify(documentRepository, never()).save(any(DocumentEntity.class));
        verify(vectorStore, never()).add(anyList());

        log.info("✅ Тест превышения размера файла пройден");
    }

    @Test
    @DisplayName("Загрузка дубликата документа")
    void testIngestDocument_WhenDocumentAlreadyExists() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);

        DocumentEntity existingDocument = DocumentEntity.builder()
                .id(TEST_ID)
                .fileName(TEST_FILE_NAME)
                .content("Existing content")
                .build();

        when(documentRepository.findByFileName(TEST_FILE_NAME))
                .thenReturn(Optional.of(existingDocument));

        // When & Then
        assertThatThrownBy(() -> documentService.ingestDocument(multipartFile, TEST_METADATA))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("уже существует");

        verify(documentRepository, never()).save(any(DocumentEntity.class));
        verify(vectorStore, never()).add(anyList());

        log.info("✅ Тест загрузки дубликата пройден");
    }

    @Test
    @DisplayName("Загрузка пустого файла")
    void testIngestDocument_WhenFileIsEmpty() throws IOException {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
        when(multipartFile.getBytes()).thenReturn("".getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> documentService.ingestDocument(multipartFile, TEST_METADATA))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Файл пуст");

        verify(documentRepository, never()).save(any(DocumentEntity.class));
        verify(vectorStore, never()).add(anyList());

        log.info("✅ Тест загрузки пустого файла пройден");
    }

    @Test
    @DisplayName("Загрузка с null файлом")
    void testIngestDocument_WithNullFile() {
        // When & Then
        assertThatThrownBy(() -> documentService.ingestDocument(null, TEST_METADATA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File cannot be null");

        log.info("✅ Тест загрузки с null файлом пройден");
    }

    @Test
    @DisplayName("Загрузка с пустым именем файла")
    void testIngestDocument_WithEmptyFileName() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("");

        // When & Then
        assertThatThrownBy(() -> documentService.ingestDocument(multipartFile, TEST_METADATA))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Имя файла отсутствует");

        log.info("✅ Тест загрузки с пустым именем файла пройден");
    }

    @Test
    @DisplayName("Загрузка с null именем файла")
    void testIngestDocument_WithNullFileName() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> documentService.ingestDocument(multipartFile, TEST_METADATA))
                .isInstanceOf(DocumentIngestionException.class)
                .hasMessageContaining("Имя файла отсутствует");

        log.info("✅ Тест загрузки с null именем файла пройден");
    }

    @Test
    @DisplayName("Загрузка изображения через OCR")
    void testIngestDocument_WithImageFile() throws DocumentIngestionException, IOException {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
        when(multipartFile.getBytes()).thenReturn(TEST_CONTENT.getBytes(StandardCharsets.UTF_8));

        String ocrText = "Extracted text from image via OCR";
        when(ocrService.isImageFile(any(MultipartFile.class))).thenReturn(true);
        when(ocrService.extractText(any(MultipartFile.class))).thenReturn(ocrText);
        when(documentRepository.findByFileName(TEST_FILE_NAME)).thenReturn(Optional.empty());

        DocumentEntity savedEntity = DocumentEntity.builder()
                .id(TEST_ID)
                .fileName(TEST_FILE_NAME)
                .content(ocrText)
                .metadata(TEST_METADATA)
                .build();

        when(documentRepository.save(any(DocumentEntity.class))).thenReturn(savedEntity);
        doNothing().when(vectorStore).add(anyList());

        // When
        documentService.ingestDocument(multipartFile, TEST_METADATA);

        // Then
        verify(ocrService).extractText(any(MultipartFile.class));
        verify(documentRepository).save(any(DocumentEntity.class));
        verify(vectorStore).add(anyList());

        log.info("✅ Тест загрузки изображения через OCR пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ reIngestDocument
    // ============================================================

    @Test
    @DisplayName("Успешная перезагрузка документа")
    void testReIngestDocument_Success() throws DocumentIngestionException, IOException {
        // Given
        DocumentEntity existingDocument = DocumentEntity.builder()
                .id(TEST_ID)
                .fileName(TEST_FILE_NAME)
                .content("Old content")
                .build();

        doNothing().when(documentRepository).deleteByFileName(TEST_FILE_NAME);

        DocumentEntity savedEntity = DocumentEntity.builder()
                .id(TEST_ID)
                .fileName(TEST_FILE_NAME)
                .content(TEST_CONTENT)
                .metadata(TEST_METADATA)
                .build();

        doAnswer(invocation -> {
            documentRepository.deleteByFileName(TEST_FILE_NAME);
            documentRepository.save(savedEntity);
            vectorStore.add(anyList());
            return null;
        }).when(documentService).reIngestDocument(any(MultipartFile.class), anyString());

        // When
        documentService.reIngestDocument(multipartFile, TEST_METADATA);

        // Then
        verify(documentRepository).deleteByFileName(TEST_FILE_NAME);
        verify(documentRepository).save(any(DocumentEntity.class));
        verify(vectorStore).add(anyList());

        log.info("✅ Тест перезагрузки документа пройден");
    }

    @Test
    @DisplayName("Перезагрузка несуществующего документа")
    void testReIngestDocument_WhenNotExists() throws DocumentIngestionException, IOException {
        // Given
        DocumentEntity savedEntity = DocumentEntity.builder()
                .id(TEST_ID)
                .fileName(TEST_FILE_NAME)
                .content(TEST_CONTENT)
                .metadata(TEST_METADATA)
                .build();

        doAnswer(invocation -> {
            documentRepository.save(savedEntity);
            vectorStore.add(anyList());
            return null;
        }).when(documentService).reIngestDocument(any(MultipartFile.class), anyString());

        // When
        documentService.reIngestDocument(multipartFile, TEST_METADATA);

        // Then
        verify(documentRepository, never()).deleteByFileName(TEST_FILE_NAME);
        verify(documentRepository).save(any(DocumentEntity.class));
        verify(vectorStore).add(anyList());

        log.info("✅ Тест перезагрузки несуществующего документа пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ CRUD ОПЕРАЦИЙ
    // ============================================================

    @Test
    @DisplayName("Получение всех документов")
    void testGetAllDocuments() {
        // Given
        DocumentEntity document1 = DocumentEntity.builder()
                .id(1L)
                .fileName("doc1.txt")
                .content("Content 1")
                .build();
        DocumentEntity document2 = DocumentEntity.builder()
                .id(2L)
                .fileName("doc2.txt")
                .content("Content 2")
                .build();

        when(documentRepository.findAll()).thenReturn(List.of(document1, document2));

        // When
        List<DocumentEntity> result = documentService.getAllDocuments();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(DocumentEntity::getFileName)
                .containsExactly("doc1.txt", "doc2.txt");
        verify(documentRepository).findAll();

        log.info("✅ Тест получения всех документов пройден");
    }

    @Test
    @DisplayName("Получение документа по ID")
    void testGetDocument_Success() {
        // Given
        DocumentEntity document = DocumentEntity.builder()
                .id(TEST_ID)
                .fileName(TEST_FILE_NAME)
                .content(TEST_CONTENT)
                .build();

        when(documentRepository.findById(TEST_ID)).thenReturn(Optional.of(document));

        // When
        Optional<DocumentEntity> result = documentService.getDocument(TEST_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TEST_ID);
        assertThat(result.get().getFileName()).isEqualTo(TEST_FILE_NAME);
        verify(documentRepository).findById(TEST_ID);

        log.info("✅ Тест получения документа по ID пройден");
    }

    @Test
    @DisplayName("Получение документа с null ID")
    void testGetDocument_WithNullId() {
        // When & Then
        assertThatThrownBy(() -> documentService.getDocument(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document ID cannot be null");

        log.info("✅ Тест получения документа с null ID пройден");
    }

    @Test
    @DisplayName("Получение документа по имени файла")
    void testGetDocumentByFileName_Success() {
        // Given
        DocumentEntity document = DocumentEntity.builder()
                .id(TEST_ID)
                .fileName(TEST_FILE_NAME)
                .content(TEST_CONTENT)
                .build();

        when(documentRepository.findByFileName(TEST_FILE_NAME))
                .thenReturn(Optional.of(document));

        // When
        Optional<DocumentEntity> result = documentService.getDocumentByFileName(TEST_FILE_NAME);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFileName()).isEqualTo(TEST_FILE_NAME);
        verify(documentRepository).findByFileName(TEST_FILE_NAME);

        log.info("✅ Тест получения документа по имени файла пройден");
    }

    @Test
    @DisplayName("Проверка существования документа (существует)")
    void testDocumentExists_WhenExists() {
        // Given
        when(documentRepository.findByFileName(TEST_FILE_NAME))
                .thenReturn(Optional.of(new DocumentEntity()));

        // When
        boolean exists = documentService.documentExists(TEST_FILE_NAME);

        // Then
        assertThat(exists).isTrue();
        verify(documentRepository).findByFileName(TEST_FILE_NAME);

        log.info("✅ Тест проверки существования документа (true) пройден");
    }

    @Test
    @DisplayName("Проверка существования документа (не существует)")
    void testDocumentExists_WhenNotExists() {
        // Given
        when(documentRepository.findByFileName(TEST_FILE_NAME))
                .thenReturn(Optional.empty());

        // When
        boolean exists = documentService.documentExists(TEST_FILE_NAME);

        // Then
        assertThat(exists).isFalse();
        verify(documentRepository).findByFileName(TEST_FILE_NAME);

        log.info("✅ Тест проверки существования документа (false) пройден");
    }

    @Test
    @DisplayName("Удаление документа по ID")
    void testDeleteDocument_Success() {
        // Given
        when(documentRepository.existsById(TEST_ID)).thenReturn(true);

        // When
        boolean result = documentService.deleteDocument(TEST_ID);

        // Then
        assertThat(result).isTrue();
        verify(documentRepository).existsById(TEST_ID);
        verify(documentRepository).deleteById(TEST_ID);

        log.info("✅ Тест удаления документа по ID пройден");
    }

    @Test
    @DisplayName("Удаление несуществующего документа по ID")
    void testDeleteDocument_NotFound() {
        // Given
        when(documentRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

        // When
        boolean result = documentService.deleteDocument(NON_EXISTENT_ID);

        // Then
        assertThat(result).isFalse();
        verify(documentRepository).existsById(NON_EXISTENT_ID);
        verify(documentRepository, never()).deleteById(NON_EXISTENT_ID);

        log.info("✅ Тест удаления несуществующего документа по ID пройден");
    }

    @Test
    @DisplayName("Удаление документа с null ID")
    void testDeleteDocument_WithNullId() {
        // When & Then
        assertThatThrownBy(() -> documentService.deleteDocument(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document ID cannot be null");

        log.info("✅ Тест удаления с null ID пройден");
    }

    @Test
    @DisplayName("Удаление документа по имени файла")
    void testDeleteDocumentByFileName_Success() {
        // Given
        DocumentEntity document = DocumentEntity.builder()
                .id(TEST_ID)
                .fileName(TEST_FILE_NAME)
                .build();

        when(documentRepository.findByFileName(TEST_FILE_NAME))
                .thenReturn(Optional.of(document));

        // When
        boolean result = documentService.deleteDocumentByFileName(TEST_FILE_NAME);

        // Then
        assertThat(result).isTrue();
        verify(documentRepository).findByFileName(TEST_FILE_NAME);
        verify(documentRepository).deleteByFileName(TEST_FILE_NAME);

        log.info("✅ Тест удаления документа по имени файла пройден");
    }

    @Test
    @DisplayName("Удаление несуществующего документа по имени")
    void testDeleteDocumentByFileName_NotFound() {
        // Given
        when(documentRepository.findByFileName(TEST_FILE_NAME))
                .thenReturn(Optional.empty());

        // When
        boolean result = documentService.deleteDocumentByFileName(TEST_FILE_NAME);

        // Then
        assertThat(result).isFalse();
        verify(documentRepository).findByFileName(TEST_FILE_NAME);
        verify(documentRepository, never()).deleteByFileName(TEST_FILE_NAME);

        log.info("✅ Тест удаления несуществующего документа по имени пройден");
    }

    @Test
    @DisplayName("Удаление документа с null именем файла")
    void testDeleteDocumentByFileName_WithNullName() {
        // When & Then
        assertThatThrownBy(() -> documentService.deleteDocumentByFileName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File name cannot be null or empty");

        log.info("✅ Тест удаления с null именем пройден");
    }

    @Test
    @DisplayName("Удаление документа с пустым именем файла")
    void testDeleteDocumentByFileName_WithEmptyName() {
        // When & Then
        assertThatThrownBy(() -> documentService.deleteDocumentByFileName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File name cannot be null or empty");

        log.info("✅ Тест удаления с пустым именем пройден");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ ОЧИСТКИ
    // ============================================================

    @Test
    @DisplayName("Очистка всех документов (только для тестов)")
    void testClearAllDocuments_Success() {
        // Given
        System.setProperty("spring.profiles.active", "test");
        doNothing().when(documentRepository).deleteAll();

        // When
        documentService.clearAllDocuments();

        // Then
        verify(documentRepository).deleteAll();

        log.info("✅ Тест очистки всех документов пройден");
    }

    @Test
    @DisplayName("Очистка всех документов - ошибка вне тестового профиля")
    void testClearAllDocuments_WhenNotInTestProfile() {
        // Given
        System.setProperty("spring.profiles.active", "production");

        // When & Then
        assertThatThrownBy(() -> documentService.clearAllDocuments())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clearAllDocuments() can only be used in test profile");

        verify(documentRepository, never()).deleteAll();

        log.info("✅ Тест очистки вне тестового профиля пройден");
    }
}