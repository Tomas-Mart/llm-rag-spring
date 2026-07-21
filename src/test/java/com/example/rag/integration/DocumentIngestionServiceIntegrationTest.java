package com.example.rag.integration;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
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
     */
    @Autowired
    private DocumentIngestionService ingestionService;

    /**
     * Репозиторий для работы с документами.
     */
    @Autowired
    private DocumentRepository documentRepository;

    /**
     * Настройка перед запуском всех тестов.
     * Ожидает загрузку модели Ollama.
     */
    @BeforeAll
    static void setup() {
        System.out.println("⏳ Ожидаем загрузку модели Ollama...");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> OLLAMA_CONTAINER.isRunning());
    }

    /**
     * Проверяет загрузку документа с интеграционным тестированием.
     *
     * @throws Exception если ошибка при создании файла
     */
    @Test
    @Description("Проверка загрузки документа с интеграционным тестированием")
    @Story("Загрузка одного документа")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("RAG-006")
    void testIngestDocument_Integration() throws Exception {
        // Arrange
        String content = """
                This is an integration support document.
                It should be processed by the real VectorStore and Database.
                Spring AI makes RAG applications easy.
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-support.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ingestionService.ingestDocument(file, "integration-support");

        // Assert
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(1);

        DocumentEntity savedDoc = documents.getFirst();
        assertThat(savedDoc.getFileName()).isEqualTo("integration-support.txt");
        assertThat(savedDoc.getContent()).isEqualTo(content);
        assertThat(savedDoc.getMetadata()).isEqualTo("integration-support");
        assertThat(savedDoc.getCreatedAt()).isNotNull();

        System.out.println("✅ Интеграционный тест успешно завершен");
        System.out.println("📄 Документ сохранен с ID: " + savedDoc.getId());
    }

    /**
     * Проверяет загрузку множественных документов.
     *
     * @throws Exception если ошибка при создании файлов
     */
    @Test
    @Description("Проверка загрузки множественных документов")
    @Story("Загрузка нескольких документов")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("RAG-007")
    void testIngestMultipleDocuments() throws Exception {
        // Arrange
        String[] contents = {
                "First document for testing.",
                "Second document for testing.",
                "Third document for testing."
        };

        // Act
        for (int i = 0; i < contents.length; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "doc-" + i + ".txt",
                    "text/plain",
                    contents[i].getBytes(StandardCharsets.UTF_8)
            );
            ingestionService.ingestDocument(file, "batch-support-" + i);
        }

        // Assert
        List<DocumentEntity> documents = documentRepository.findAll();
        assertThat(documents).hasSize(3);

        System.out.println("✅ Тест множественной загрузки пройден");
        System.out.println("📄 Загружено документов: " + documents.size());
    }
}