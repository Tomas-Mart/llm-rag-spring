package com.example.rag.integration.controller.web;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.controller.web.DocumentController;
import com.example.rag.entity.DocumentEntity;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для {@link DocumentController}.
 *
 * <p>Проверяет работу DocumentController с реальным Spring контекстом,
 * реальной базой данных PostgreSQL и реальным VectorStore.</p>
 *
 * <p>Использует {@link BaseIntegrationTestWithContainers} для поднятия
 * контейнера с PostgreSQL и pgvector.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Успешная загрузка документа</li>
 *   <li>Загрузка с метаданными</li>
 *   <li>Загрузка без метаданных</li>
 *   <li>Пустой файл</li>
 *   <li>Файл слишком большого размера</li>
 *   <li>Загрузка существующего документа (без force)</li>
 *   <li>Принудительная перезагрузка документа (force=true)</li>
 *   <li>Обработка ошибок</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see DocumentController
 * @see BaseIntegrationTestWithContainers
 * @since 1.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Epic("Интеграционные тесты")
@Feature("Web Контроллеры")
class DocumentControllerIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String UPLOAD_URL = "/api/documents/upload";
    private static final String REDIRECT_URL = "/";
    private static final String FILE_PARAM = "file";
    private static final String METADATA_PARAM = "metadata";
    private static final String FORCE_PARAM = "force";
    private static final String FLASH_MESSAGE_ATTR = "message";
    private static final String TEST_FILE_NAME = "integration-test.txt";
    private static final String TEST_FILE_CONTENT = "This is an integration test document content.";
    private static final String TEST_METADATA = "{\"author\":\"integration-test\",\"category\":\"web-controller\"}";

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentService documentService;

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
    @Description("Проверка успешной загрузки документа")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Документ должен успешно загружаться")
    void testUploadDocument_Success() throws Exception {
        // Arrange
        MockMultipartFile file = createMultipartFile(TEST_FILE_NAME, TEST_FILE_CONTENT);

        // Act & Assert
        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, TEST_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists(FLASH_MESSAGE_ATTR));

        // Проверяем, что документ сохранен в БД
        assertThat(documentRepository.findByFileName(TEST_FILE_NAME))
                .as("Документ должен быть сохранен в БД")
                .isPresent();

        log.info("✅ Документ успешно загружен");
    }

    @Test
    @Description("Проверка загрузки документа без метаданных")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Документ должен загружаться без метаданных")
    void testUploadDocument_WithoutMetadata() throws Exception {
        // Arrange
        MockMultipartFile file = createMultipartFile(TEST_FILE_NAME, TEST_FILE_CONTENT);

        // Act & Assert
        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists(FLASH_MESSAGE_ATTR));

        // Проверяем, что документ сохранен в БД
        assertThat(documentRepository.findByFileName(TEST_FILE_NAME))
                .as("Документ должен быть сохранен в БД")
                .isPresent();

        log.info("✅ Документ загружен без метаданных");
    }

    @Test
    @Description("Проверка загрузки пустого файла")
    @Story("Валидация")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Пустой файл должен отклоняться")
    void testUploadEmptyFile_ShouldBeRejected() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                FILE_PARAM,
                "empty.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(emptyFile)
                        .param(METADATA_PARAM, TEST_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attribute(FLASH_MESSAGE_ATTR, "❌ Файл не выбран или пустой"));

        // Проверяем, что документ НЕ сохранен в БД
        assertThat(documentRepository.findAll())
                .as("Пустой файл не должен быть сохранен")
                .isEmpty();

        log.info("✅ Пустой файл отклонен");
    }

    @Test
    @Description("Проверка загрузки файла больше 10MB")
    @Story("Валидация")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Файл больше 10MB должен отклоняться")
    void testUploadLargeFile_ShouldBeRejected() throws Exception {
        // Arrange
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                FILE_PARAM,
                "large.txt",
                MediaType.TEXT_PLAIN_VALUE,
                largeContent
        );

        // Act & Assert
        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(largeFile)
                        .param(METADATA_PARAM, TEST_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists(FLASH_MESSAGE_ATTR));

        // Проверяем, что документ НЕ сохранен в БД
        assertThat(documentRepository.findAll())
                .as("Большой файл не должен быть сохранен")
                .isEmpty();

        log.info("✅ Большой файл отклонен");
    }

    @Test
    @Description("Проверка загрузки существующего документа без force")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Существующий документ без force должен предупреждать")
    void testUploadExistingDocument_WithoutForce_ShouldWarn() throws Exception {
        // Arrange - сначала загружаем документ
        MockMultipartFile file = createMultipartFile(TEST_FILE_NAME, TEST_FILE_CONTENT);
        documentService.ingestDocument(file, TEST_METADATA);

        // Act - пытаемся загрузить повторно
        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, TEST_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists(FLASH_MESSAGE_ATTR));

        // Проверяем, что документ все еще один
        assertThat(documentRepository.findAll())
                .as("Документ не должен дублироваться")
                .hasSize(1);

        log.info("✅ Существующий документ без force обработан корректно");
    }

    @Test
    @Description("Проверка принудительной перезагрузки документа")
    @Story("Загрузка документов")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Существующий документ с force=true должен перезагружаться")
    void testUploadExistingDocument_WithForce_ShouldReload() throws Exception {
        // Arrange - сначала загружаем документ
        String oldContent = "Old content";
        String newContent = "New content after reload";
        MockMultipartFile firstFile = createMultipartFile(TEST_FILE_NAME, oldContent);
        documentService.ingestDocument(firstFile, TEST_METADATA);

        // Проверяем, что старый контент сохранен
        DocumentEntity oldDoc = documentRepository.findByFileName(TEST_FILE_NAME).orElseThrow();
        assertThat(oldDoc.getContent()).isEqualTo(oldContent);

        // Act - перезагружаем с force=true
        MockMultipartFile newFile = createMultipartFile(TEST_FILE_NAME, newContent);
        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(newFile)
                        .param(METADATA_PARAM, TEST_METADATA)
                        .param(FORCE_PARAM, "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists(FLASH_MESSAGE_ATTR));

        // Проверяем, что контент обновлен
        DocumentEntity updatedDoc = documentRepository.findByFileName(TEST_FILE_NAME).orElseThrow();
        assertThat(updatedDoc.getContent())
                .as("Контент должен быть обновлен")
                .isEqualTo(newContent);

        log.info("✅ Документ успешно перезагружен");
    }

    @Test
    @Description("Проверка обработки ошибки DocumentIngestionException")
    @Story("Обработка ошибок")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Ошибка загрузки должна быть обработана")
    void testUploadDocument_WhenIngestionFails_ShouldHandleError() throws Exception {
        // Arrange - создаем файл с именем, которое вызовет ошибку
        MockMultipartFile file = createMultipartFile("invalid/name.txt", TEST_FILE_CONTENT);

        // Act & Assert - проверяем что ошибка обработана
        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param(METADATA_PARAM, TEST_METADATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URL))
                .andExpect(flash().attributeExists(FLASH_MESSAGE_ATTR));

        log.info("✅ Ошибка загрузки обработана корректно");
    }

    @Test
    @Description("Проверка что DocumentService внедрен в контроллер")
    @Story("Архитектура")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("DocumentService должен быть успешно внедрен")
    void testDocumentServiceIsInjected() {
        // Assert
        assertThat(documentService)
                .as("DocumentService должен быть внедрен в контроллер")
                .isNotNull();

        log.info("✅ DocumentService успешно внедрен");
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
                FILE_PARAM,
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}