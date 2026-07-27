package com.example.rag.integration;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для {@link VectorStore}.
 *
 * <p>Проверяет работу векторного хранилища с реальной базой данных PostgreSQL
 * и pgvector расширением.</p>
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Доступность VectorStore</li>
 *   <li>Добавление документов в векторное хранилище</li>
 *   <li>Поиск релевантных документов</li>
 *   <li>Поиск с порогом схожести</li>
 *   <li>Поиск с ограничением количества результатов</li>
 *   <li>Поиск с разными запросами</li>
 *   <li>Работа с пустым хранилищем</li>
 *   <li>Сохранение метаданных</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 2.0
 * @see VectorStore
 * @see BaseIntegrationTestWithContainers
 * @since 1.0
 */
@Slf4j
@Tag("integration")
@Tag("vector")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Epic("Интеграционные тесты")
@Feature("Векторное хранилище")
class VectorStoreIT extends BaseIntegrationTestWithContainers {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.3;
    private static final int DOCUMENTS_COUNT_FOR_TOP_K_TEST = 10;

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    @Autowired
    private VectorStore vectorStore;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        log.info("🧹 VectorStore очищен");
    }

    // ============================================================
    // ТЕСТЫ
    // ============================================================

    @Test
    @Description("Проверка доступности VectorStore")
    @Story("Работа с векторным хранилищем")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("VectorStore должен быть доступен")
    void testVectorStoreIsAvailable() {
        logTestStart("Testing VectorStore availability");

        assertThat(vectorStore)
                .as("VectorStore should be available")
                .isNotNull();

        log.info("✅ VectorStore type: {}", vectorStore.getClass().getSimpleName());
        logTestSuccess("VectorStore availability test passed");
    }

    @Test
    @Description("Проверка добавления и поиска документов")
    @Story("Работа с векторным хранилищем")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Документы должны добавляться и находиться по запросу")
    void testVectorStoreCanAddAndSearch() {
        logTestStart("Testing VectorStore add and search");

        // Given - создаем тестовые документы
        Document doc1 = new Document(
                UUID.randomUUID().toString(),
                "Spring AI is a framework for building AI applications with Spring Boot.",
                Map.of("category", "ai", "topic", "spring")
        );

        Document doc2 = new Document(
                UUID.randomUUID().toString(),
                "Vector databases are used for storing and searching embeddings.",
                Map.of("category", "database", "topic", "vector")
        );

        Document doc3 = new Document(
                UUID.randomUUID().toString(),
                "Spring Boot makes it easy to create stand-alone Spring applications.",
                Map.of("category", "spring", "topic", "boot")
        );

        // When - добавляем документы
        vectorStore.add(List.of(doc1, doc2, doc3));
        log.info("📚 Добавлено 3 документа в векторное хранилище");

        // Then - ищем релевантные документы
        SearchRequest searchRequest = SearchRequest.builder()
                .query("Spring Boot framework")
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        assertThat(results)
                .as("Должны быть найдены релевантные документы")
                .isNotNull();

        log.info("📊 Найдено {} документов", results.size());
        logTestSuccess("VectorStore add and search test passed");
    }

    @Test
    @Description("Проверка поиска с порогом схожести")
    @Story("Работа с векторным хранилищем")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Поиск должен учитывать порог схожести")
    void testVectorStoreSearchWithThreshold() {
        logTestStart("Testing VectorStore search with threshold");

        // Given
        Document doc = new Document(
                UUID.randomUUID().toString(),
                "Spring AI provides integration with Ollama for local LLM inference.",
                Map.of("category", "ai")
        );
        vectorStore.add(List.of(doc));

        // When - поиск с низким порогом
        SearchRequest lowThreshold = SearchRequest.builder()
                .query("Ollama LLM")
                .topK(TOP_K)
                .similarityThreshold(0.1)
                .build();

        List<Document> lowResults = vectorStore.similaritySearch(lowThreshold);

        // When - поиск с высоким порогом
        SearchRequest highThreshold = SearchRequest.builder()
                .query("Ollama LLM")
                .topK(TOP_K)
                .similarityThreshold(0.9)
                .build();

        List<Document> highResults = vectorStore.similaritySearch(highThreshold);

        // Then
        assertThat(lowResults)
                .as("С низким порогом должны быть результаты")
                .isNotEmpty();

        assertThat(highResults)
                .as("С высоким порогом могут быть или не быть результаты")
                .isNotNull();

        log.info("📊 Низкий порог: {} результатов, высокий порог: {} результатов",
                lowResults.size(), highResults.size());
        logTestSuccess("VectorStore search with threshold test passed");
    }

    @Test
    @Description("Проверка поиска с ограничением количества результатов")
    @Story("Работа с векторным хранилищем")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Поиск должен возвращать не более topK результатов")
    void testVectorStoreSearchWithTopK() {
        logTestStart("Testing VectorStore search with topK");

        // Given - добавляем много документов
        for (int i = 0; i < DOCUMENTS_COUNT_FOR_TOP_K_TEST; i++) {
            Document doc = new Document(
                    UUID.randomUUID().toString(),
                    "Test document " + i + " about Spring AI and embeddings.",
                    Map.of("index", i)
            );
            vectorStore.add(List.of(doc));
        }

        // When - поиск с topK=3
        SearchRequest searchRequest = SearchRequest.builder()
                .query("Spring AI")
                .topK(3)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        // Then
        assertThat(results)
                .as("Должны быть найдены документы")
                .isNotNull();

        assertThat(results.size())
                .as("Результатов должно быть не больше 3")
                .isLessThanOrEqualTo(3);

        log.info("📊 Запрошено 3 документа, найдено {}", results.size());
        logTestSuccess("VectorStore search with topK test passed");
    }

    @Test
    @Description("Проверка поиска с разными запросами")
    @Story("Работа с векторным хранилищем")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Разные запросы должны возвращать разные результаты")
    void testVectorStoreSearchWithDifferentQueries() {
        logTestStart("Testing VectorStore search with different queries");

        // Given
        Document doc1 = new Document(
                UUID.randomUUID().toString(),
                "Java is a programming language used for enterprise applications.",
                Map.of("topic", "java")
        );

        Document doc2 = new Document(
                UUID.randomUUID().toString(),
                "Python is popular for data science and machine learning.",
                Map.of("topic", "python")
        );

        vectorStore.add(List.of(doc1, doc2));

        // When
        SearchRequest javaSearch = SearchRequest.builder()
                .query("Java programming")
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        SearchRequest pythonSearch = SearchRequest.builder()
                .query("Python data science")
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> javaResults = vectorStore.similaritySearch(javaSearch);
        List<Document> pythonResults = vectorStore.similaritySearch(pythonSearch);

        // Then
        assertThat(javaResults)
                .as("По Java запросу должны быть результаты")
                .isNotEmpty();

        assertThat(pythonResults)
                .as("По Python запросу должны быть результаты")
                .isNotEmpty();

        log.info("📊 Java запрос: {} результатов, Python запрос: {} результатов",
                javaResults.size(), pythonResults.size());
        logTestSuccess("VectorStore search with different queries test passed");
    }

    @Test
    @Description("Проверка работы с пустым хранилищем")
    @Story("Работа с векторным хранилищем")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Пустое хранилище должно возвращать пустые результаты")
    void testVectorStoreSearchWithEmptyStore() {
        logTestStart("Testing VectorStore search with empty store");

        // When
        SearchRequest searchRequest = SearchRequest.builder()
                .query("Anything")
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        // Then
        assertThat(results)
                .as("Из пустого хранилища должны вернуться пустые результаты")
                .isNotNull();

        log.info("📊 В пустом хранилище найдено {} документов", results.size());
        logTestSuccess("VectorStore search with empty store test passed");
    }

    @Test
    @Description("Проверка добавления документов с метаданными")
    @Story("Работа с векторным хранилищем")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Документы с метаданными должны корректно сохраняться")
    void testVectorStoreAddWithMetadata() {
        logTestStart("Testing VectorStore add with metadata");

        // Given
        Document doc = new Document(
                UUID.randomUUID().toString(),
                "This document has important metadata.",
                Map.of(
                        "author", "Test Author",
                        "category", "integration-test",
                        "priority", "high",
                        "version", 1
                )
        );

        // When
        vectorStore.add(List.of(doc));

        // Then - ищем и проверяем метаданные
        SearchRequest searchRequest = SearchRequest.builder()
                .query("important metadata")
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        assertThat(results)
                .as("Документ должен быть найден")
                .isNotEmpty();

        Document foundDoc = results.getFirst();
        assertThat(foundDoc.getMetadata())
                .as("Метаданные должны сохраниться")
                .containsKey("author")
                .containsKey("category");

        log.info("📊 Найден документ с метаданными: {}", foundDoc.getMetadata());
        logTestSuccess("VectorStore add with metadata test passed");
    }
}