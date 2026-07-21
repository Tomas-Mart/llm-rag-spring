package com.example.rag.support;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.example.rag.Application;
import com.example.rag.service.DocumentIngestionService;
import com.example.rag.service.RagService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Абстрактный базовый класс для всех интеграционных тестов.
 * Обеспечивает единую конфигурацию Spring контекста, общие моки и вспомогательные методы.
 *
 * <p>Основные возможности:
 * <ul>
 *   <li>Загрузка Spring контекста с тестовым профилем</li>
 *   <li>Предоставление общих моков для внешних зависимостей</li>
 *   <li>Настройка MockMvc для тестирования контроллеров</li>
 *   <li>Содержит общие методы для проверки контекста и моков</li>
 *   <li>Использует SLF4J для структурированного логирования</li>
 * </ul>
 *
 * <p>Пример использования:
 * <pre>{@code
 * @SpringBootTest
 * @AutoConfigureMockMvc
 * class MyIntegrationTest extends BaseIntegrationTest {
 *
 *     @Test
 *     void testSomething() {
 *         assertAllMocksCreated();
 *         // Выполняем тест
 *     }
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest(classes = Application.class)
public abstract class BaseIntegrationTest {

    /**
     * Логгер для всех тестовых классов.
     * Используется для единообразного логирования во всех наследниках.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * MockMvc для тестирования контроллеров.
     * Автоматически внедряется Spring.
     */
    @Autowired
    protected MockMvc mockMvc;

    /**
     * Мок для RagService.
     * Используется для изоляции тестов от реальной RAG логики.
     */
    @MockBean
    protected RagService ragService;

    /**
     * Мок для DocumentIngestionService.
     * Используется для изоляции тестов от реальной логики загрузки документов.
     */
    @MockBean
    protected DocumentIngestionService ingestionService;

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
     */
    @MockBean
    protected VectorStore vectorStore;

    /**
     * Инициализация перед каждым тестом.
     * Выводит информацию о запуске теста.
     */
    @BeforeEach
    void setUpBase() {
        logger.info("🚀 Running integration test: {} - Starting execution", getTestName());
        logger.debug("📋 Test class: {}", getClass().getName());
    }

    /**
     * Проверяет, что все основные моки созданы и не равны {@code null}.
     *
     * <p>Проверяемые моки:
     * <ul>
     *   <li>{@link #ragService} - мок RagService</li>
     *   <li>{@link #ingestionService} - мок DocumentIngestionService</li>
     *   <li>{@link #ollamaApi} - мок Ollama API</li>
     *   <li>{@link #chatModel} - мок Ollama Chat Model</li>
     *   <li>{@link #vectorStore} - мок Vector Store</li>
     * </ul>
     *
     * <p>Должен вызываться в каждом тестовом классе для гарантии,
     * что Spring создал все необходимые моки.
     *
     * @throws AssertionError если любой из моков равен {@code null}
     */
    protected void assertAllMocksCreated() {
        assertThat(ragService)
                .as("RagService mock should be created")
                .isNotNull();

        assertThat(ingestionService)
                .as("DocumentIngestionService mock should be created")
                .isNotNull();

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
        logger.debug("   - RagService: {}", ragService.getClass().getSimpleName());
        logger.debug("   - DocumentIngestionService: {}", ingestionService.getClass().getSimpleName());
        logger.debug("   - OllamaApi: {}", ollamaApi.getClass().getSimpleName());
        logger.debug("   - OllamaChatModel: {}", chatModel.getClass().getSimpleName());
        logger.debug("   - VectorStore: {}", vectorStore.getClass().getSimpleName());
    }

    /**
     * Проверяет, что MockMvc доступен.
     *
     * @throws AssertionError если {@link #mockMvc} равен {@code null}
     */
    protected void assertMockMvcAvailable() {
        assertThat(mockMvc)
                .as("MockMvc should be available")
                .isNotNull();
        logger.debug("MockMvc is available");
    }

    /**
     * Проверяет, что все обязательные компоненты загружены.
     * Объединяет проверку всех моков.
     */
    protected void assertAllComponentsLoaded() {
        assertAllMocksCreated();
        assertMockMvcAvailable();
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