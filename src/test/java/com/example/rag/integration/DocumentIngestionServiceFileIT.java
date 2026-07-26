package com.example.rag.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.service.DocumentIngestionService;
import com.example.rag.support.BaseIntegrationTest;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Интеграционный тест для проверки загрузки документов из реальных файлов.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет работу {@link DocumentIngestionService} с файловой системой
 * и реальной базой данных PostgreSQL.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Загрузка документа из реального файла</li>
 *   <li>Загрузка нескольких файлов из директории</li>
 *   <li>Обработка пустых файлов</li>
 *   <li>Предотвращение дубликатов файлов</li>
 * </ul>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Использует реальную PostgreSQL через {@link BaseIntegrationTest}</li>
 *   <li>Создает временные файлы через {@code @TempDir}</li>
 *   <li>Транзакции автоматически откатываются после каждого теста</li>
 *   <li>Все аннотации наследуются от {@link BaseIntegrationTest}</li>
 * </ul>
 *
 * <h2>Пример запуска</h2>
 * <pre>{@code
 * // Запустить все тесты
 * mvn test -Dtest=DocumentIngestionServiceFileIT
 *
 * // Запустить конкретный тест
 * mvn test -Dtest=DocumentIngestionServiceFileIT#testIngestDocumentFromRealFile
 * }</pre>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see DocumentIngestionService
 * @see BaseIntegrationTest
 * @since 1.0
 */
@Slf4j
@Epic("Интеграционные тесты")
@Feature("Загрузка документов из файлов")
class DocumentIngestionServiceFileIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String REAL_FILE_NAME = "real-support.txt";
    private static final String DUPLICATE_FILE_NAME = "duplicate.txt";
    private static final int MULTIPLE_FILES_COUNT = 3;

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    protected DocumentIngestionService ingestionService;

    @Autowired
    protected DocumentRepository documentRepository;

    @TempDir
    Path tempDir;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        log.info("🧹 Repository cleared");
    }

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    /**
     * Проверяет загрузку документа из реального файла.
     */
    @Test
    @Description("Проверка загрузки документа из реального файла")
    @Story("Работа с файловой системой")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-008")
    void testIngestDocumentFromRealFile() throws IOException {
        logTestStart("Testing real file ingestion");

        // 1. Создаем реальный файл
        String content = "This is a real file content for testing Spring AI RAG system.";
        Path testFile = createFile(REAL_FILE_NAME, content);

        // 2. Создаем MultipartFile и загружаем
        MockMultipartFile multipartFile = createMultipartFileFromPath(testFile);
        ingestionService.ingestDocument(multipartFile, "real-file-support");

        // 3. Проверяем сохранение
        List<DocumentEntity> savedDocuments = documentRepository.findAll();
        assertThat(savedDocuments)
                .as("Document should be saved in repository")
                .isNotEmpty();

        DocumentEntity savedDoc = savedDocuments.getFirst();
        assertThat(savedDoc.getFileName())
                .as("File name should match")
                .isEqualTo(REAL_FILE_NAME);

        log.info("✅ Document saved with ID: {}", savedDoc.getId());
        logTestSuccess("Real file ingestion completed");
    }

    /**
     * Проверяет загрузку нескольких файлов из директории.
     */
    @Test
    @Description("Проверка загрузки нескольких файлов из директории")
    @Story("Работа с файловой системой")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-009")
    void testIngestMultipleFilesFromDirectory() throws IOException {
        logTestStart("Testing multiple files ingestion");

        // 1. Создаем файлы
        String[] contents = {
                "File 1: Spring AI basics for RAG",
                "File 2: Vector databases and embeddings",
                "File 3: RAG architecture and implementation"
        };

        for (int i = 0; i < contents.length; i++) {
            Path file = createFile("doc-" + (i + 1) + ".txt", contents[i]);
            MockMultipartFile multipartFile = createMultipartFileFromPath(file);
            ingestionService.ingestDocument(multipartFile, "batch-support");
            log.debug("📄 Loaded document {}", i + 1);
        }

        // 2. Проверяем сохранение
        List<DocumentEntity> savedDocuments = documentRepository.findAll();
        assertThat(savedDocuments)
                .as("Should have saved all {} documents", MULTIPLE_FILES_COUNT)
                .hasSize(MULTIPLE_FILES_COUNT);

        // 3. Проверяем имена файлов
        List<String> fileNames = savedDocuments.stream()
                .map(DocumentEntity::getFileName)
                .toList();

        assertThat(fileNames)
                .as("All file names should be correct")
                .containsExactlyInAnyOrder("doc-1.txt", "doc-2.txt", "doc-3.txt");

        log.info("✅ Loaded {} files", savedDocuments.size());
        logTestSuccess("Multiple files ingestion completed");
    }

    /**
     * Проверяет, что сервис корректно обрабатывает пустые файлы.
     */
    @Test
    @Description("Проверка обработки пустого файла")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-010")
    void testIngestEmptyFile() throws IOException {
        logTestStart("Testing empty file handling");

        // 1. Создаем пустой файл
        Path emptyFile = createFile("empty.txt", "");

        // 2. Создаем MultipartFile и пытаемся загрузить
        MockMultipartFile multipartFile = createMultipartFileFromPath(emptyFile);

        // 3. Проверяем, что выбрасывается исключение
        assertThatCode(() -> ingestionService.ingestDocument(multipartFile, "empty-file"))
                .as("Empty file should throw exception")
                .isInstanceOf(Exception.class);

        // 4. Проверяем, что документ не сохранен
        List<DocumentEntity> savedDocuments = documentRepository.findAll();
        assertThat(savedDocuments)
                .as("Empty file should not be saved")
                .isEmpty();

        logTestSuccess("Empty file handling works correctly");
    }

    /**
     * Проверяет, что дубликаты файлов не создаются.
     */
    @Test
    @Description("Проверка предотвращения дубликатов файлов")
    @Story("Работа с файловой системой")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-011")
    void testDuplicateFileIngestion() throws IOException {
        logTestStart("Testing duplicate file prevention");

        // 1. Создаем файл
        String content = "This is a test file for duplicate checking.";
        Path testFile = createFile(DUPLICATE_FILE_NAME, content);

        // 2. Создаем MultipartFile
        MockMultipartFile multipartFile = createMultipartFileFromPath(testFile);

        // 3. Загружаем первый раз - должно быть успешно
        ingestionService.ingestDocument(multipartFile, "duplicate-test");
        List<DocumentEntity> firstSave = documentRepository.findAll();
        assertThat(firstSave)
                .as("First save should succeed")
                .hasSize(1);

        // 4. Загружаем второй раз - должно выбросить исключение
        assertThatCode(() -> ingestionService.ingestDocument(multipartFile, "duplicate-test"))
                .as("Duplicate file should throw exception")
                .isInstanceOf(Exception.class);

        // 5. Проверяем, что дубликат не создан
        List<DocumentEntity> secondSave = documentRepository.findAll();
        assertThat(secondSave)
                .as("Duplicate should not be created")
                .hasSize(1);

        logTestSuccess("Duplicate file prevention works correctly");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Создает файл во временной директории.
     *
     * @param fileName имя файла
     * @param content  содержимое файла
     * @return путь к созданному файлу
     * @throws IOException если ошибка записи
     */
    private Path createFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content, StandardOpenOption.CREATE);
        return file;
    }

    /**
     * Создает MockMultipartFile из файла.
     *
     * @param filePath путь к файлу
     * @return MockMultipartFile
     * @throws IOException если ошибка чтения
     */
    private MockMultipartFile createMultipartFileFromPath(Path filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(filePath);
        return new MockMultipartFile(
                "file",
                filePath.getFileName().toString(),
                "text/plain",
                fileBytes
        );
    }
}