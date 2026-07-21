package com.example.rag.support;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.example.rag.Application;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.service.DocumentIngestionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Абстрактный базовый класс для тестов загрузки документов из файлов.
 *
 * <p>Использует моки для внешних зависимостей, чтобы изолировать тесты
 * от реальных сервисов и базы данных. Это позволяет тестировать логику
 * загрузки документов без необходимости в реальной векторной базе данных.</p>
 *
 * <p>Основные возможности:
 * <ul>
 *   <li>Загрузка Spring контекста с тестовым профилем</li>
 *   <li>Предоставление моков для внешних зависимостей</li>
 *   <li>Автоматическое внедрение DocumentIngestionService и DocumentRepository</li>
 *   <li>Единое логирование для всех тестов</li>
 * </ul>
 * </p>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see DocumentIngestionService
 * @see DocumentRepository
 * @since 1.0
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public abstract class BaseFileTest {

    /**
     * Логгер для всех тестовых классов.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Сервис для загрузки документов.
     * Автоматически внедряется Spring.
     */
    @Autowired
    protected DocumentIngestionService ingestionService;

    /**
     * Репозиторий для работы с документами.
     * Автоматически внедряется Spring.
     */
    @Autowired
    protected DocumentRepository documentRepository;

    /**
     * Мок для Ollama API.
     * Используется для изоляции тестов от реального Ollama сервера.
     */
    @MockBean
    protected OllamaApi ollamaApi;

    /**
     * Мок для Ollama Chat Model.
     * Используется для изоляции тестов от реальной LLM.
     */
    @MockBean
    protected OllamaChatModel chatModel;

    /**
     * Мок для Vector Store.
     * Используется для изоляции тестов от реальной векторной базы данных.
     *
     * <p>Этот мок критически важен, так как:
     * <ul>
     *   <li>Предотвращает создание реального подключения к pgvector</li>
     *   <li>Позволяет тестировать без PostgreSQL</li>
     *   <li>Ускоряет выполнение тестов</li>
     *   <li>Избегает проблем с H2 и CREATE EXTENSION</li>
     * </ul>
     * </p>
     */
    @MockBean
    protected VectorStore vectorStore;

    /**
     * Инициализация перед каждым тестом.
     * Выводит информацию о запуске теста в лог.
     */
    @BeforeEach
    void setUpBase() {
        logger.info("🚀 Running file test: {}", getTestName());
        logger.debug("📋 Test class: {}", getClass().getName());
    }

    /**
     * Проверяет, что все основные моки созданы и не равны {@code null}.
     *
     * <p>Проверяемые моки:
     * <ul>
     *   <li>{@link #ollamaApi} - мок Ollama API</li>
     *   <li>{@link #chatModel} - мок Ollama Chat Model</li>
     *   <li>{@link #vectorStore} - мок Vector Store</li>
     * </ul>
     * </p>
     *
     * @throws AssertionError если любой из моков равен {@code null}
     */
    protected void assertMocksCreated() {
        assertThat(ollamaApi)
                .as("OllamaApi mock should be created")
                .isNotNull();

        assertThat(chatModel)
                .as("OllamaChatModel mock should be created")
                .isNotNull();

        assertThat(vectorStore)
                .as("VectorStore mock should be created")
                .isNotNull();

        logger.info("All mocks created successfully");
        logger.debug("   - OllamaApi: {}", ollamaApi.getClass().getSimpleName());
        logger.debug("   - OllamaChatModel: {}", chatModel.getClass().getSimpleName());
        logger.debug("   - VectorStore: {}", vectorStore.getClass().getSimpleName());
    }

    /**
     * Проверяет, что все обязательные компоненты загружены.
     * Объединяет проверку всех моков.
     *
     * <p>Рекомендуется вызывать в начале каждого теста для гарантии
     * корректной загрузки всех необходимых компонентов.</p>
     *
     * @throws AssertionError если любой из компонентов не загружен
     */
    protected void assertAllComponentsLoaded() {
        assertMocksCreated();
        assertThat(ingestionService)
                .as("DocumentIngestionService should be available")
                .isNotNull();
        assertThat(documentRepository)
                .as("DocumentRepository should be available")
                .isNotNull();

        logger.info("All components loaded successfully");
    }

    /**
     * Возвращает имя класса теста для использования в логировании.
     *
     * @return простое имя класса теста (без имени пакета)
     */
    protected String getTestName() {
        return getClass().getSimpleName();
    }

    /**
     * Возвращает полное имя класса теста для детального логирования.
     *
     * @return полное имя класса теста (с именем пакета)
     */
    protected String getFullTestName() {
        return getClass().getName();
    }

    /**
     * Логирует начало выполнения теста с дополнительной информацией.
     *
     * @param message дополнительное сообщение для логирования
     */
    protected void logTestStart(String message) {
        logger.info("🚀 [{}] {}", getTestName(), message);
    }

    /**
     * Логирует успешное завершение теста.
     *
     * @param message дополнительное сообщение для логирования
     */
    protected void logTestSuccess(String message) {
        logger.info("✅ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует предупреждение в тесте.
     *
     * @param message сообщение предупреждения
     */
    protected void logTestWarning(String message) {
        logger.warn("⚠️ [{}] {}", getTestName(), message);
    }

    /**
     * Логирует ошибку в тесте.
     *
     * @param message   сообщение об ошибке
     * @param throwable исключение для логирования
     */
    protected void logTestError(String message, Throwable throwable) {
        logger.error("❌ [{}] {}", getTestName(), message, throwable);
    }
}