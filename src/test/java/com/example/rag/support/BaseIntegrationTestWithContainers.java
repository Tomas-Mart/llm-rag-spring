package com.example.rag.support;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.example.rag.Application;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.Mockito.mock;

/**
 * Расширенный базовый класс для интеграционных тестов с моком EmbeddingModel.
 *
 * <h2>Назначение</h2>
 * <p>Предоставляет инфраструктуру для интеграционных тестов, которым требуется
 * реальный {@code VectorStore}, но не нужны реальные эмбеддинги от Ollama.</p>
 *
 * <h2>Наследование</h2>
 * <p>Наследует всю мощность {@link BaseIntegrationTest}:
 * <ul>
 *   <li>✅ Testcontainers с PostgreSQL и pgvector</li>
 *   <li>✅ Реальный {@code DataSource} и {@code VectorStore}</li>
 *   <li>✅ Моки для Ollama API и Chat Model</li>
 *   <li>✅ Транзакционность (@Transactional)</li>
 *   <li>✅ Все проверки компонентов</li>
 *   <li>✅ Утилиты логирования</li>
 * </ul>
 *
 * <h2>Добавленная функциональность</h2>
 * <ul>
 *   <li>✅ Мок для {@link EmbeddingModel} с {@code @Primary}</li>
 *   <li>✅ Изоляция от реальных LLM вызовов</li>
 *   <li>✅ Детерминированные результаты тестов</li>
 *   <li>✅ Ускорение выполнения тестов</li>
 *   <li>✅ Методы проверки состояния контейнеров</li>
 * </ul>
 *
 * <h2>Когда использовать</h2>
 * <ul>
 *   <li>✅ Нужен реальный {@code VectorStore} для проверки сохранения эмбеддингов</li>
 *   <li>✅ Не нужны реальные эмбеддинги от Ollama</li>
 *   <li>✅ Тестируется интеграция с БД без LLM</li>
 *   <li>✅ Нужна изоляция от внешних LLM сервисов</li>
 * </ul>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * @Slf4j
 * @Epic("Интеграционные тесты")
 * @Feature("Загрузка документов")
 * class DocumentIngestionServiceIntegrationTest extends BaseIntegrationTestWithContainers {
 *
 *     @Autowired
 *     private DocumentIngestionService ingestionService;
 *
 *     @Test
 *     void testIngestDocument() {
 *         if (!isOllamaRunning()) {
 *             log.warn("Ollama not running, skipping test");
 *             return;
 *         }
 *         // Использует реальный VectorStore с моком EmbeddingModel
 *         ingestionService.ingestDocument(file, "test");
 *     }
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see BaseIntegrationTest
 * @see EmbeddingModel
 * @see org.springframework.ai.vectorstore.VectorStore
 * @since 1.0
 */
@Slf4j
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = {Application.class, BaseIntegrationTestWithContainers.MockConfig.class})
@ActiveProfiles("integration-test")
public abstract class BaseIntegrationTestWithContainers extends BaseIntegrationTest {

    // ============================================================
    // КОНСТАНТЫ (Java 21)
    // ============================================================

    private static final String POSTGRES_IMAGE = "pgvector/pgvector:pg16";
    private static final String OLLAMA_IMAGE = "ollama/ollama:latest";
    private static final String DATABASE_NAME = "rag_integration_test";
    private static final String DATABASE_USER = "test_user";
    private static final String DATABASE_PASSWORD = "test_password";
    private static final int OLLAMA_PORT = 11434;

    // ============================================================
    // КОНТЕЙНЕРЫ
    // ============================================================

    /**
     * PostgreSQL контейнер с pgvector.
     * Переиспользуется между тестами для ускорения.
     */
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(
                    DockerImageName.parse(POSTGRES_IMAGE)
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName(DATABASE_NAME)
                    .withUsername(DATABASE_USER)
                    .withPassword(DATABASE_PASSWORD)
                    .withReuse(true);

    /**
     * Ollama контейнер для LLM.
     * Опциональный - запускается только если нужен.
     */
    @Container
    protected static final GenericContainer<?> OLLAMA_CONTAINER =
            new GenericContainer<>(DockerImageName.parse(OLLAMA_IMAGE))
                    .withExposedPorts(OLLAMA_PORT)
                    .withCommand("serve")
                    .withReuse(true);

    // ============================================================
    // ДИНАМИЧЕСКАЯ КОНФИГУРАЦИЯ
    // ============================================================

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

        var jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
        log.info("🐘 Testcontainers PostgreSQL started on: {}", jdbcUrl);
    }

    // ============================================================
    // МЕТОДЫ ПРОВЕРКИ СОСТОЯНИЯ КОНТЕЙНЕРОВ
    // ============================================================

    /**
     * Проверяет, запущен ли PostgreSQL контейнер.
     *
     * @return {@code true} если контейнер запущен, {@code false} в противном случае
     */
    protected boolean isPostgresRunning() {
        return POSTGRES_CONTAINER != null && POSTGRES_CONTAINER.isRunning();
    }

    /**
     * Проверяет, запущен ли Ollama контейнер.
     *
     * @return {@code true} если контейнер запущен, {@code false} в противном случае
     */
    protected boolean isOllamaRunning() {
        return OLLAMA_CONTAINER != null && OLLAMA_CONTAINER.isRunning();
    }

    /**
     * Возвращает порт Ollama.
     *
     * @return порт Ollama
     * @throws IllegalStateException если контейнер не запущен
     */
    protected int getOllamaPort() {
        if (!isOllamaRunning()) {
            throw new IllegalStateException("Ollama container is not running");
        }
        return OLLAMA_CONTAINER.getMappedPort(OLLAMA_PORT);
    }

    /**
     * Возвращает JDBC URL PostgreSQL.
     *
     * @return JDBC URL
     * @throws IllegalStateException если контейнер не запущен
     */
    protected String getPostgresJdbcUrl() {
        if (!isPostgresRunning()) {
            throw new IllegalStateException("PostgreSQL container is not running");
        }
        return POSTGRES_CONTAINER.getJdbcUrl();
    }

    /**
     * Возвращает хост PostgreSQL.
     *
     * @return хост PostgreSQL
     * @throws IllegalStateException если контейнер не запущен
     */
    protected String getPostgresHost() {
        if (!isPostgresRunning()) {
            throw new IllegalStateException("PostgreSQL container is not running");
        }
        return POSTGRES_CONTAINER.getHost();
    }

    /**
     * Возвращает порт PostgreSQL.
     *
     * @return порт PostgreSQL
     * @throws IllegalStateException если контейнер не запущен
     */
    protected int getPostgresPort() {
        if (!isPostgresRunning()) {
            throw new IllegalStateException("PostgreSQL container is not running");
        }
        return POSTGRES_CONTAINER.getMappedPort(5432);
    }

    // ============================================================
    // КОНФИГУРАЦИЯ МОКОВ
    // ============================================================

    /**
     * Конфигурация моков для интеграционных тестов.
     *
     * <p>Создает мок для {@link EmbeddingModel} с {@code @Primary},
     * что гарантирует его использование вместо реального бина.</p>
     *
     * <p>Такой подход позволяет:
     * <ul>
     *   <li>Изолировать тесты от реальных LLM вызовов</li>
     *   <li>Ускорить выполнение тестов</li>
     *   <li>Обеспечить детерминированные результаты</li>
     * </ul>
     */
    @Configuration
    @Profile("integration-test")
    public static class MockConfig {

        /**
         * Создает мок для {@link EmbeddingModel}.
         *
         * <p>Мок используется вместо реального {@link EmbeddingModel}
         * для изоляции тестов от Ollama. Все методы мока возвращают
         * пустые или фиктивные значения.</p>
         *
         * <p>Аннотация {@code @Primary} гарантирует, что этот мок
         * будет выбран вместо любого другого бина того же типа.</p>
         *
         * @return мок {@link EmbeddingModel} с {@code @Primary}
         */
        @Bean
        @Primary
        public EmbeddingModel embeddingModel() {
            log.info("🔧 Creating mock EmbeddingModel for integration tests");
            return mock(EmbeddingModel.class);
        }
    }
}