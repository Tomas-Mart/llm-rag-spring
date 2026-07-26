package com.example.rag.integration;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.service.DocumentIngestionService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционный тест для проверки загрузки документов.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет работу {@link DocumentIngestionService} с реальной базой данных
 * PostgreSQL через существующий контейнер.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Загрузка одного документа</li>
 *   <li>Загрузка множественных документов</li>
 *   <li>Загрузка пустого документа</li>
 *   <li>Загрузка большого документа</li>
 * </ul>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Использует реальную PostgreSQL с pgvector</li>
 *   <li>Транзакционная изоляция для автоматического отката</li>
 *   <li>Очистка репозитория перед каждым тестом</li>
 *   <li>Все аннотации наследуются от {@link BaseIntegrationTestWithContainers}</li>
 * </ul>
 *
 * <h2>Пример запуска</h2>
 * <pre>{@code
 * // Запустить все тесты
 * mvn test -Dtest=DocumentIngestionServiceIT
 *
 * // Запустить конкретный тест
 * mvn test -Dtest=DocumentIngestionServiceIT#testIngestDocument
 * }</pre>
 *
 * @author RAG Application Team
 * @version 6.0
 * @see DocumentIngestionService
 * @see BaseIntegrationTestWithContainers
 * @since 1.0
 */
@Slf4j
@Epic("Интеграционные тесты")
@Feature("Загрузка документов")
class DocumentIngestionServiceIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final int LARGE_DOCUMENT_REPEAT_COUNT = 2500;
    private static final String TEST_FILE_NAME = "integration-support.txt";
    private static final String METADATA = "integration-support";

    private static final String[] MULTIPLE_CONTENTS = {
            "First document for integration testing.",
            "Second document for integration testing.",
            "Third document for integration testing."
    };

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private DocumentIngestionService ingestionService;

    @Autowired
    private DocumentRepository documentRepository;

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

    @Test
    @Description("Проверка загрузки одного документа")
    @Story("Загрузка одного документа")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-006")
    void testIngestDocument() {
        logTestStart("Testing single document ingestion");

        // 1. Создаем и загружаем документ
        String content = """
                This is an integration support document.
                It should be processed by the real VectorStore and Database.
                Spring AI makes RAG applications easy.
                """;

        MockMultipartFile file = createMultipartFile(TEST_FILE_NAME, content);

        assertThatCode(() -> ingestionService.ingestDocument(file, METADATA))
                .doesNotThrowAnyException();

        // 2. Проверяем сохранение
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents)
                .as("Should have exactly one document saved")
                .hasSize(1);

        DocumentEntity savedDoc = documents.getFirst();

        // ИСПРАВЛЕНО: Используем trim() для сравнения текста
        assertThat(savedDoc)
                .as("Saved document should have correct data")
                .satisfies(doc -> {
                    assertThat(doc.getFileName()).isEqualTo(TEST_FILE_NAME);
                    assertThat(doc.getContent().trim()).isEqualTo(content.trim());
                    assertThat(doc.getMetadata()).isEqualTo(METADATA);
                    assertThat(doc.getCreatedAt()).isNotNull();
                });

        log.info("✅ Document saved with ID: {}", savedDoc.getId());
        logTestSuccess("Single document ingestion completed");
    }

    @Test
    @Description("Проверка загрузки множественных документов")
    @Story("Загрузка нескольких документов")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-007")
    void testIngestMultipleDocuments() {
        logTestStart("Testing multiple documents ingestion");

        // 1. Загружаем документы
        for (int i = 0; i < MULTIPLE_CONTENTS.length; i++) {
            String fileName = "doc-" + i + ".txt";
            String metadata = "batch-support-" + i;
            MockMultipartFile file = createMultipartFile(fileName, MULTIPLE_CONTENTS[i]);

            ingestionService.ingestDocument(file, metadata);
            log.debug("📄 Loaded document {}", i + 1);
        }

        // 2. Проверяем сохранение
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents)
                .as("Should have saved {} documents", MULTIPLE_CONTENTS.length)
                .hasSize(MULTIPLE_CONTENTS.length);

        // 3. Проверяем каждый документ
        for (int i = 0; i < MULTIPLE_CONTENTS.length; i++) {
            final int index = i;
            DocumentEntity doc = documents.get(index);

            // ИСПРАВЛЕНО: Используем trim() для сравнения текста
            assertThat(doc)
                    .as("Document {} should have correct data", index)
                    .satisfies(d -> {
                        assertThat(d.getFileName()).isEqualTo("doc-" + index + ".txt");
                        assertThat(d.getContent().trim()).isEqualTo(MULTIPLE_CONTENTS[index].trim());
                        assertThat(d.getMetadata()).isEqualTo("batch-support-" + index);
                        assertThat(d.getCreatedAt()).isNotNull();
                    });
        }

        log.info("✅ Loaded {} documents", documents.size());
        logTestSuccess("Multiple documents ingestion completed");
    }

    @Test
    @Description("Проверка загрузки пустого документа")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-010")
    void testIngestEmptyDocument() {
        logTestStart("Testing empty document ingestion");

        // ИСПРАВЛЕНО: Ожидаем исключение при загрузке пустого файла
        MockMultipartFile file = createMultipartFile("empty.txt", "");

        assertThatThrownBy(() -> ingestionService.ingestDocument(file, "empty-document"))
                .isInstanceOf(com.example.rag.exception.DocumentIngestionException.class)
                .hasMessageContaining("Файл пуст");

        // Проверяем, что документ не был сохранен
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents)
                .as("Empty document should NOT be saved")
                .isEmpty();

        logTestSuccess("Empty document ingestion correctly rejected");
    }

    @Test
    @Description("Проверка загрузки большого документа")
    @Story("Обработка больших документов")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-011")
    void testIngestLargeDocument() {
        logTestStart("Testing large document ingestion");

        // 1. Создаем и загружаем большой документ (~50KB)
        String largeContent = "Integration test large content. ".repeat(LARGE_DOCUMENT_REPEAT_COUNT);
        MockMultipartFile file = createMultipartFile("large-document.txt", largeContent);

        assertThatCode(() -> ingestionService.ingestDocument(file, "large-document"))
                .doesNotThrowAnyException();

        // 2. Проверяем сохранение
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents)
                .as("Large document should be saved")
                .hasSize(1);

        DocumentEntity savedDoc = documents.getFirst();

        // ИСПРАВЛЕНО: Используем trim() для сравнения длины
        assertThat(savedDoc.getContent().trim().length())
                .as("Content length should match")
                .isEqualTo(largeContent.trim().length());

        log.info("✅ Large document saved ({} KB)", savedDoc.getContent().length() / 1024);
        logTestSuccess("Large document ingestion completed");
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
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}