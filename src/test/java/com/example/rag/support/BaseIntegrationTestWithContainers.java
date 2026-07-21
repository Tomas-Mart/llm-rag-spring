package com.example.rag.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.example.rag.Application;

/**
 * Абстрактный базовый класс для интеграционных тестов с использованием Testcontainers.
 * Обеспечивает единую конфигурацию контейнеров для PostgreSQL и Ollama.
 *
 * <p>Основные возможности:
 * <ul>
 *   <li>Запуск PostgreSQL контейнера с pgvector</li>
 *   <li>Запуск Ollama контейнера</li>
 *   <li>Динамическая настройка свойств приложения</li>
 * </ul>
 *
 * <p>Пример использования:
 * <pre>{@code
 * @SpringBootTest
 * @Testcontainers
 * class MyIntegrationTest extends BaseIntegrationTestWithContainers {
 *
 *     @Test
 *     void testSomething() {
 *         // Используем контейнеры
 *     }
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTestWithContainers {

    /**
     * Логгер для всех тестовых классов.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Контейнер PostgreSQL с pgvector.
     * Используется для тестирования с реальной базой данных.
     *
     * <p>Используется {@link DockerImageName#asCompatibleSubstituteFor(String)}
     * для совместимости с Testcontainers PostgreSQL API.</p>
     */
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:pg16")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("rag_db")
                    .withUsername("rag_user")
                    .withPassword("rag_pass")
                    .withInitScript("init-vector.sql");

    /**
     * Контейнер Ollama.
     * Используется для тестирования с реальной LLM.
     *
     * <p>Используется {@code GenericContainer} для запуска Ollama.
     * Модель qwen2.5-coder:7b загружается при старте.</p>
     */
    @Container
    protected static final GenericContainer<?> OLLAMA_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("ollama/ollama:latest"))
                    .withExposedPorts(11434)
                    .withCommand("ollama pull qwen2.5-coder:7b")
                    .withReuse(true);

    /**
     * Динамическая настройка свойств приложения.
     * Подставляет URL контейнеров в конфигурацию Spring.
     *
     * @param registry реестр свойств для динамической настройки
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.ai.ollama.base-url", () ->
                "http://localhost:" + OLLAMA_CONTAINER.getMappedPort(11434));
    }

    /**
     * Проверяет, что PostgreSQL контейнер запущен и доступен.
     *
     * @return {@code true} если контейнер запущен, {@code false} в противном случае
     */
    protected boolean isPostgresRunning() {
        return POSTGRES_CONTAINER.isRunning();
    }

    /**
     * Проверяет, что Ollama контейнер запущен и доступен.
     *
     * @return {@code true} если контейнер запущен, {@code false} в противном случае
     */
    protected boolean isOllamaRunning() {
        return OLLAMA_CONTAINER.isRunning();
    }

    /**
     * Возвращает JDBC URL для подключения к PostgreSQL.
     *
     * @return JDBC URL контейнера
     */
    protected String getPostgresJdbcUrl() {
        return POSTGRES_CONTAINER.getJdbcUrl();
    }

    /**
     * Возвращает порт Ollama контейнера.
     *
     * @return порт Ollama
     */
    protected int getOllamaPort() {
        return OLLAMA_CONTAINER.getMappedPort(11434);
    }

    /**
     * Возвращает полный URL Ollama.
     *
     * @return URL Ollama
     */
    protected String getOllamaUrl() {
        return "http://localhost:" + getOllamaPort();
    }

    /**
     * Возвращает имя класса теста для использования в логировании.
     *
     * @return простое имя класса теста
     */
    protected String getTestName() {
        return getClass().getSimpleName();
    }
}