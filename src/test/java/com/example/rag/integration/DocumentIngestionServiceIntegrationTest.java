package com.example.rag.integration;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Интеграционный тест для проверки загрузки документов с использованием Testcontainers.
 * Проверяет работу {@link DocumentIngestionService} с реальной базой данных и Ollama.
 *
 * <p>Тестируемые сценарии:
 * <ul>
 *   <li>Загрузка документа с интеграционным тестированием</li>
 *   <li>Загрузка множественных документов</li>
 * </ul>
 *
 * <p>Особенности:
 * <ul>
 *   <li>Используется реальная PostgreSQL с pgvector через Testcontainers</li>
 *   <li>Транзакционная изоляция (@Transactional) для автоматического отката</li>
 *   <li>Очистка репозитория перед каждым тестом для независимости</li>
 *   <li>Ожидание только PostgreSQL, Ollama опционально</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Transactional
@Epic("Интеграционные тесты")
@Feature("Загрузка документов")
class DocumentIngestionServiceIntegrationTest extends BaseIntegrationTestWithContainers {

    /**
     * Сервис для загрузки документов.
     * Автоматически внедряется Spring.
     */
    @Autowired
    private DocumentIngestionService ingestionService;

    /**
     * Репозиторий для работы с документами.
     * Автоматически внедряется Spring.
     */
    @Autowired
    private DocumentRepository documentRepository;

    /**
     * Очищает репозиторий перед каждым тестом.
     * Обеспечивает независимость тестов друг от друга.
     */
    @BeforeEach
    void setUp() {
        // Очищаем репозиторий для изоляции тестов
        documentRepository.deleteAll();
        logger.info("🧹 Repository cleared for test: {}", getTestName());
    }

    /**
     * Проверяет загрузку документа с интеграционным тестированием.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Подключение к реальной PostgreSQL</li>
     *   <li>Сохранение документа в базу данных</li>
     *   <li>Корректность сохраненных данных</li>
     * </ul>
     *
     * @throws Exception если ошибка при создании файла
     */
    @Test
    @Description("Проверка загрузки документа с интеграционным тестированием")
    @Story("Загрузка одного документа")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-006")
    void testIngestDocument_Integration() throws Exception {
        // Проверяем, что PostgreSQL контейнер запущен и доступен
        assertThat(isPostgresRunning())
                .as("PostgreSQL container should be running")
                .isTrue();

        logger.info("🐘 PostgreSQL is running: {}", getPostgresJdbcUrl());

        // Подготовка тестового документа
        String content = """
                This is an integration support document.
                It should be processed by the real VectorStore and Database.
                Spring AI makes RAG applications easy.
                """;

        // Создаем MockMultipartFile для имитации загрузки файла
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-support.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        // Выполняем загрузку документа, проверяем что исключение не выбрасывается
        assertThatCode(() -> ingestionService.ingestDocument(file, "integration-support"))
                .doesNotThrowAnyException();

        // Проверяем, что документ сохранен в базе данных
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents)
                .as("Should have exactly one document saved")
                .hasSize(1);

        // Проверяем корректность сохраненных данных
        DocumentEntity savedDoc = documents.getFirst();
        assertThat(savedDoc)
                .as("Saved document should have correct data")
                .satisfies(doc -> {
                    assertThat(doc.getFileName()).isEqualTo("integration-support.txt");
                    assertThat(doc.getContent()).isEqualTo(content);
                    assertThat(doc.getMetadata()).isEqualTo("integration-support");
                    assertThat(doc.getCreatedAt()).isNotNull();
                });

        logger.info("✅ Интеграционный тест успешно завершен");
        logger.info("📄 Документ сохранен с ID: {}", savedDoc.getId());
        logger.info("📄 Размер содержимого: {} символов", savedDoc.getContent().length());
    }

    /**
     * Проверяет загрузку множественных документов.
     *
     * <p>Тест проверяет:
     * <ul>
     *   <li>Загрузку нескольких документов подряд</li>
     *   <li>Сохранение всех документов в базе данных</li>
     *   <li>Корректность данных каждого документа</li>
     * </ul>
     *
     * @throws Exception если ошибка при создании файлов
     */
    @Test
    @Description("Проверка загрузки множественных документов")
    @Story("Загрузка нескольких документов")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-007")
    void testIngestMultipleDocuments() throws Exception {
        // Проверяем, что PostgreSQL контейнер запущен
        assertThat(isPostgresRunning())
                .as("PostgreSQL container should be running")
                .isTrue();

        // Подготовка тестовых данных
        String[] contents = {
                "First document for integration testing.",
                "Second document for integration testing.",
                "Third document for integration testing."
        };

        String[] metadata = {
                "batch-support-0",
                "batch-support-1",
                "batch-support-2"
        };

        // Загружаем каждый документ
        for (int i = 0; i < contents.length; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "doc-" + i + ".txt",
                    "text/plain",
                    contents[i].getBytes(StandardCharsets.UTF_8)
            );

            ingestionService.ingestDocument(file, metadata[i]);

            // Логируем прогресс
            logger.debug("📄 Загружен документ {}: doc-{}.txt", i + 1, i);
        }

        // Проверяем, что все документы сохранены
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents)
                .as("Should have saved exactly {} documents", contents.length)
                .hasSize(contents.length);

        // Проверяем корректность каждого документа
        for (int i = 0; i < contents.length; i++) {
            final int index = i;  // ← final переменная для лямбды
            DocumentEntity doc = documents.get(index);

            assertThat(doc)
                    .as("Document {} should have correct data", index)
                    .satisfies(d -> {
                        assertThat(d.getFileName()).isEqualTo("doc-" + index + ".txt");
                        assertThat(d.getContent()).isEqualTo(contents[index]);
                        assertThat(d.getMetadata()).isEqualTo(metadata[index]);
                        assertThat(d.getCreatedAt()).isNotNull();
                    });
        }

        logger.info("✅ Тест множественной загрузки пройден");
        logger.info("📄 Загружено документов: {}", documents.size());

        // Выводим информацию о загруженных документах
        for (int i = 0; i < documents.size(); i++) {
            final int index = i;
            DocumentEntity doc = documents.get(index);
            logger.debug("   {}: {} (ID: {}, размер: {} символов)",
                    index + 1,
                    doc.getFileName(),
                    doc.getId(),
                    doc.getContent().length());
        }
    }

    /**
     * Проверяет загрузку документа с пустым содержимым.
     *
     * <p>Проверяет обработку пограничного случая - пустой документ.
     */
    @Test
    @Description("Проверка загрузки пустого документа")
    @Story("Обработка граничных случаев")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-010")
    void testIngestEmptyDocument() throws Exception {
        // Проверяем, что PostgreSQL контейнер запущен
        assertThat(isPostgresRunning()).isTrue();

        // Создаем пустой документ
        String emptyContent = "";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                emptyContent.getBytes(StandardCharsets.UTF_8)
        );

        // Выполняем загрузку
        assertThatCode(() -> ingestionService.ingestDocument(file, "empty-document"))
                .doesNotThrowAnyException();

        // Проверяем, что документ сохранен
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents)
                .as("Empty document should be saved")
                .hasSize(1);

        DocumentEntity savedDoc = documents.getFirst();
        assertThat(savedDoc.getContent())
                .as("Content should be empty")
                .isEmpty();

        logger.info("✅ Тест с пустым документом пройден");
        logger.info("📄 Пустой документ сохранен с ID: {}", savedDoc.getId());
    }

    /**
     * Проверяет загрузку документа с очень большим содержимым.
     *
     * <p>Проверяет, что система может обрабатывать большие документы.
     */
    @Test
    @Description("Проверка загрузки большого документа")
    @Story("Обработка больших документов")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-011")
    void testIngestLargeDocument() throws Exception {
        // Проверяем, что PostgreSQL контейнер запущен
        assertThat(isPostgresRunning()).isTrue();

        // Создаем большой документ (~50KB)
        String largeContent = "Integration test large content. ".repeat(2500);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-document.txt",
                "text/plain",
                largeContent.getBytes(StandardCharsets.UTF_8)
        );

        // Выполняем загрузку
        assertThatCode(() -> ingestionService.ingestDocument(file, "large-document"))
                .doesNotThrowAnyException();

        // Проверяем, что документ сохранен
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents)
                .as("Large document should be saved")
                .hasSize(1);

        DocumentEntity savedDoc = documents.getFirst();
        assertThat(savedDoc.getContent().length())
                .as("Content length should match")
                .isEqualTo(largeContent.length());

        logger.info("✅ Тест с большим документом пройден");
        logger.info("📄 Размер документа: {} символов ({} KB)",
                savedDoc.getContent().length(),
                savedDoc.getContent().length() / 1024);
    }
}