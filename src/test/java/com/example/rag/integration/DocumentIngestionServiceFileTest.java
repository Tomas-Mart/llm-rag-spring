package com.example.rag.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.service.DocumentIngestionService;
import com.example.rag.support.BaseFileTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки загрузки документов из реальных файлов.
 * Проверяет работу {@link DocumentIngestionService} с файловой системой.
 *
 * <p>Данный класс наследуется от {@link BaseFileTest}, который предоставляет
 * все необходимые моки для изоляции тестов от внешних зависимостей.
 * Это позволяет тестировать логику загрузки документов без необходимости
 * в реальной векторной базе данных или LLM.</p>
 *
 * <p>Тестируемые сценарии:
 * <ul>
 *   <li>Загрузка документа из реального файла</li>
 *   <li>Загрузка нескольких файлов из директории</li>
 * </ul>
 * </p>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see BaseFileTest
 * @see DocumentIngestionService
 * @see DocumentRepository
 * @since 1.0
 */
@Epic("Интеграционные тесты")
@Feature("Загрузка документов из файлов")
class DocumentIngestionServiceFileTest extends BaseFileTest {  // ← ДОБАВИТЬ public

    /**
     * Временная директория для тестовых файлов.
     *
     * <p>Аннотация {@code @TempDir} создает временную директорию,
     * которая автоматически удаляется после завершения теста.</p>
     */
    @TempDir
    Path tempDir;

    /**
     * Проверяет загрузку документа из реального файла.
     *
     * <p>Тест выполняет следующие шаги:
     * <ol>
     *   <li>Создает реальный файл во временной директории</li>
     *   <li>Читает содержимое файла в {@link MockMultipartFile}</li>
     *   <li>Вызывает {@link DocumentIngestionService#ingestDocument}</li>
     *   <li>Проверяет, что документ сохранен в репозитории</li>
     * </ol>
     * </p>
     *
     * <p>Этот тест проверяет, что сервис корректно обрабатывает
     * реальные файлы из файловой системы.</p>
     *
     * @throws IOException если возникает ошибка при чтении или записи файла
     */
    @Test
    @Description("Проверка загрузки документа из реального файла")
    @Story("Работа с файловой системой")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-008")
    void testIngestDocumentFromRealFile() throws IOException {
        // Проверяем, что все компоненты загружены корректно
        assertAllComponentsLoaded();

        // Создаем реальный файл во временной директории
        // Используем Path для работы с файловой системой
        Path testFile = tempDir.resolve("real-support.txt");
        String content = "This is a real file content for testing.";

        // Записываем содержимое в файл
        // StandardOpenOption.CREATE - создает файл если он не существует
        Files.writeString(testFile, content, StandardOpenOption.CREATE);

        // Создаем MultipartFile из реального файла
        // Это имитирует загрузку файла через HTTP запрос
        byte[] fileBytes = Files.readAllBytes(testFile);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",                                          // Имя поля формы
                testFile.getFileName().toString(),               // Имя файла
                "text/plain",                                   // MIME тип
                fileBytes                                       // Содержимое файла
        );

        // Выполняем загрузку документа
        // Используем метаданные "real-file-support" для идентификации
        ingestionService.ingestDocument(multipartFile, "real-file-support");

        // Проверяем, что документ был сохранен
        // findAll() возвращает все документы из репозитория
        var savedDocuments = documentRepository.findAll();

        // Проверяем, что список не пустой
        assertThat(savedDocuments)
                .as("Documents should be saved in repository")
                .isNotEmpty();

        // Логируем успешное выполнение теста
        logger.info("✅ Test with real file completed successfully");
        logger.info("📁 File: {}", testFile.getFileName());
    }

    /**
     * Проверяет загрузку нескольких файлов из директории.
     *
     * <p>Тест выполняет следующие шаги:
     * <ol>
     *   <li>Создает 3 файла во временной директории</li>
     *   <li>Для каждого файла создает {@link MockMultipartFile}</li>
     *   <li>Загружает все файлы через {@link DocumentIngestionService}</li>
     *   <li>Проверяет, что все 3 документа сохранены в репозитории</li>
     * </ol>
     * </p>
     *
     * <p>Этот тест проверяет, что сервис корректно обрабатывает
     * множественные загрузки и не теряет данные.</p>
     *
     * @throws IOException если возникает ошибка при чтении или записи файла
     */
    @Test
    @Description("Проверка загрузки нескольких файлов из директории")
    @Story("Работа с файловой системой")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-009")
    void testIngestMultipleFilesFromDirectory() throws IOException {
        // Проверяем, что все компоненты загружены корректно
        assertAllComponentsLoaded();

        // Создаем массив с содержимым для 3 файлов
        String[] contents = {
                "File 1: Spring AI basics",
                "File 2: Vector databases",
                "File 3: RAG architecture"
        };

        // Загружаем каждый файл
        for (int i = 0; i < contents.length; i++) {
            // Создаем файл с уникальным именем
            // doc-1.txt, doc-2.txt, doc-3.txt
            Path file = tempDir.resolve("doc-" + (i + 1) + ".txt");

            // Записываем содержимое в файл
            Files.writeString(file, contents[i]);

            // Читаем содержимое файла
            byte[] fileBytes = Files.readAllBytes(file);

            // Создаем MultipartFile для загрузки
            MockMultipartFile multipartFile = new MockMultipartFile(
                    "file",                                          // Имя поля формы
                    file.getFileName().toString(),                   // Имя файла
                    "text/plain",                                   // MIME тип
                    fileBytes                                       // Содержимое файла
            );

            // Загружаем документ
            // Все файлы имеют одинаковые метаданные "batch-support"
            ingestionService.ingestDocument(multipartFile, "batch-support");
        }

        // Проверяем, что все 3 документа сохранены
        var savedDocuments = documentRepository.findAll();

        // Проверяем размер списка
        assertThat(savedDocuments)
                .as("Should have saved all 3 documents")
                .hasSize(3);

        // Логируем успешное выполнение теста
        logger.info("✅ Test with multiple files completed successfully");
        logger.info("📁 Loaded files: {}", savedDocuments.size());
    }
}