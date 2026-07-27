package com.example.rag.performance;

import java.util.List;
import java.util.Optional;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.repository.DocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты производительности и обнаружения N+1 проблемы в репозитории документов.
 *
 * <p>Эти тесты проверяют:
 * <ul>
 *   <li>Количество SQL запросов при различных операциях</li>
 *   <li>Наличие N+1 проблемы при загрузке связанных сущностей</li>
 *   <li>Производительность методов репозитория</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@DataJpaTest
@ActiveProfiles("test")
@Tag("performance")
class DocumentRepositoryPerformanceTest {

    @Autowired
    private DocumentRepository documentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    private static final int DOCUMENT_COUNT = 50;
    private static final int MAX_QUERIES_THRESHOLD = 3;

    @BeforeEach
    void setUp() {
        // Включаем статистику Hibernate
        entityManager.unwrap(org.hibernate.Session.class)
                .getSessionFactory()
                .getStatistics()
                .setStatisticsEnabled(true);

        // Очищаем данные перед каждым тестом
        documentRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Создает тестовые данные с документами.
     *
     * @param count количество документов
     */
    private void createTestData(int count) {
        for (int i = 0; i < count; i++) {
            DocumentEntity doc = new DocumentEntity();
            doc.setFileName("doc_" + i + ".txt");
            doc.setContent("Content " + i);
            doc.setMetadata("{\"index\": " + i + "}");
            documentRepository.save(doc);
        }
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Получает статистику Hibernate.
     */
    private Statistics getStatistics() {
        return entityManager.unwrap(org.hibernate.Session.class)
                .getSessionFactory()
                .getStatistics();
    }

    /**
     * Сбрасывает статистику.
     */
    private void clearStatistics() {
        getStatistics().clear();
    }

    /**
     * Логирует результаты теста.
     */
    private void logResults(String testName, int documentsCount, long queryCount, long duration) {
        log.info("========================================");
        log.info("📊 {}:", testName);
        log.info("   Documents: {}", documentsCount);
        log.info("   Queries: {}", queryCount);
        log.info("   Duration: {}ms", duration);
        log.info("   N+1 Problem: {}", queryCount > MAX_QUERIES_THRESHOLD ? "⚠️ YES" : "✅ NO");
        log.info("========================================");
    }

    // ==================== БАЗОВЫЕ ТЕСТЫ ====================

    @Test
    void testFindAllQueriesPerformance() {
        // Given
        createTestData(DOCUMENT_COUNT);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        List<DocumentEntity> documents = documentRepository.findAll();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findAll", documents.size(), queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(duration).isLessThan(1000);
        assertThat(documents).hasSize(DOCUMENT_COUNT);
    }

    @Test
    void testFindByFileNamePerformance() {
        // Given
        createTestData(30);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        Optional<DocumentEntity> doc = documentRepository.findByFileName("doc_15.txt");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findByFileName", doc.isPresent() ? 1 : 0, queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(duration).isLessThan(500);
        assertThat(doc).isPresent();
        assertThat(doc.get().getFileName()).isEqualTo("doc_15.txt");
    }

    @Test
    void testExistsByFileNamePerformance() {
        // Given
        createTestData(30);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        boolean exists = documentRepository.existsByFileName("doc_15.txt");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("existsByFileName", exists ? 1 : 0, queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(duration).isLessThan(500);
        assertThat(exists).isTrue();
    }

    @Test
    void testDeleteByFileNamePerformance() {
        // Given
        createTestData(30);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        documentRepository.deleteByFileName("doc_10.txt");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("deleteByFileName", 0, queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(duration).isLessThan(500);

        // Проверяем что документ удален
        Optional<DocumentEntity> deleted = documentRepository.findByFileName("doc_10.txt");
        assertThat(deleted).isEmpty();
    }

    // ==================== ТЕСТЫ С ОПТИМИЗИРОВАННЫМИ ЗАПРОСАМИ ====================

    @Test
    void testFindAllWithOptimizedNoNPlusOne() {
        // Given
        createTestData(20);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        List<DocumentEntity> documents = documentRepository.findAllWithOptimized();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findAllWithOptimized", documents.size(), queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(2);
        assertThat(documents).hasSize(20);
    }

    @Test
    void testFindByFileNameWithOptimized() {
        // Given
        createTestData(30);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        Optional<DocumentEntity> doc = documentRepository.findByFileNameWithOptimized("doc_5.txt");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findByFileNameWithOptimized", doc.isPresent() ? 1 : 0, queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(2);
        assertThat(doc).isPresent();
        assertThat(doc.get().getFileName()).isEqualTo("doc_5.txt");
    }

    @Test
    void testFindAllOrderedByFileName() {
        // Given
        createTestData(20);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        List<DocumentEntity> documents = documentRepository.findAllOrderedByFileName();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findAllOrderedByFileName", documents.size(), queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(documents).hasSize(20);

        // Проверяем сортировку
        for (int i = 0; i < documents.size() - 1; i++) {
            assertThat(documents.get(i).getFileName())
                    .isLessThan(documents.get(i + 1).getFileName());
        }
    }

    @Test
    void testFindByIdWithOptimized() {
        // Given
        createTestData(20);
        Long testId = documentRepository.findByFileName("doc_5.txt")
                .map(DocumentEntity::getId)
                .orElseThrow();
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        Optional<DocumentEntity> doc = documentRepository.findByIdWithOptimized(testId);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findByIdWithOptimized", doc.isPresent() ? 1 : 0, queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(2);
        assertThat(doc).isPresent();
        assertThat(doc.get().getId()).isEqualTo(testId);
    }

    // ==================== ТЕСТЫ МАССОВЫХ ОПЕРАЦИЙ ====================

    @Test
    void testFindAllByIdInWithOptimized() {
        // Given
        createTestData(30);
        List<Long> ids = documentRepository.findAllIds().subList(0, 5);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        List<DocumentEntity> documents = documentRepository.findAllByIdInWithOptimized(ids);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findAllByIdInWithOptimized", documents.size(), queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(2);
        assertThat(documents).hasSize(5);
    }

    @Test
    void testFindAllIdsPerformance() {
        // Given
        createTestData(50);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        List<Long> ids = documentRepository.findAllIds();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findAllIds", ids.size(), queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(ids).hasSize(50);
    }

    // ==================== ТЕСТЫ С БОЛЬШИМИ ДАННЫМИ ====================

    @Test
    @Tag("heavy")
    void testPerformanceWithLargeDataset() {
        // Given
        int largeCount = 200;
        createTestData(largeCount);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        List<DocumentEntity> documents = documentRepository.findAllWithOptimized();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("Large dataset (200 docs)", documents.size(), queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(2);
        assertThat(documents).hasSize(largeCount);
        assertThat(duration).isLessThan(2000);
    }

    // ==================== ТЕСТЫ СРАВНЕНИЯ МЕТОДОВ ====================

    @Test
    void testComparePerformanceOfFindAllMethods() {
        // Given
        createTestData(30);

        // Test 1: Обычный findAll
        clearStatistics();
        long start1 = System.currentTimeMillis();
        List<DocumentEntity> docs1 = documentRepository.findAll();
        long duration1 = System.currentTimeMillis() - start1;
        long queries1 = getStatistics().getQueryExecutionCount();

        // Test 2: findAllWithOptimized (EntityGraph)
        clearStatistics();
        long start2 = System.currentTimeMillis();
        List<DocumentEntity> docs2 = documentRepository.findAllWithOptimized();
        long duration2 = System.currentTimeMillis() - start2;
        long queries2 = getStatistics().getQueryExecutionCount();

        // Test 3: findAllOrderedByFileName (сортировка)
        clearStatistics();
        long start3 = System.currentTimeMillis();
        List<DocumentEntity> docs3 = documentRepository.findAllOrderedByFileName();
        long duration3 = System.currentTimeMillis() - start3;
        long queries3 = getStatistics().getQueryExecutionCount();

        // Then
        log.info("========================================");
        log.info("📊 PERFORMANCE COMPARISON:");
        log.info("   Method                     | Queries | Duration | Size");
        log.info("   ---------------------------|---------|----------|------");
        log.info("   findAll                    | {:7} | {:8}ms | {}",
                queries1, duration1, docs1.size());
        log.info("   findAllWithOptimized       | {:7} | {:8}ms | {}",
                queries2, duration2, docs2.size());
        log.info("   findAllOrderedByFileName   | {:7} | {:8}ms | {}",
                queries3, duration3, docs3.size());
        log.info("========================================");

        // Все методы должны возвращать одинаковое количество документов
        assertThat(docs1).hasSize(30);
        assertThat(docs2).hasSize(30);
        assertThat(docs3).hasSize(30);
    }

    // ==================== ТЕСТЫ ПОИСКА ====================

    @Test
    void testFindByFileNameNotFound() {
        // Given
        createTestData(10);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        Optional<DocumentEntity> doc = documentRepository.findByFileName("nonexistent.txt");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findByFileName (not found)", 0, queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(duration).isLessThan(500);
        assertThat(doc).isEmpty();
    }

    @Test
    void testExistsByFileNameNotFound() {
        // Given
        createTestData(10);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        boolean exists = documentRepository.existsByFileName("nonexistent.txt");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("existsByFileName (not found)", 0, queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(duration).isLessThan(500);
        assertThat(exists).isFalse();
    }

    @Test
    void testExistsByFileNameOptimizedPerformance() {
        // Given
        createTestData(30);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        boolean exists = documentRepository.existsByFileNameOptimized("doc_15.txt");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("existsByFileNameOptimized", exists ? 1 : 0, queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(duration).isLessThan(500);
        assertThat(exists).isTrue();
    }

    @Test
    void testFindByContentContainingIgnoreCase() {
        // Given
        createTestData(20);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        List<DocumentEntity> documents = documentRepository.findByContentContainingIgnoreCase("Content 5");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findByContentContainingIgnoreCase", documents.size(), queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getContent()).contains("Content 5");
    }

    @Test
    void testFindByFileNameContainingIgnoreCase() {
        // Given
        createTestData(20);
        clearStatistics();

        // When
        long startTime = System.currentTimeMillis();
        List<DocumentEntity> documents = documentRepository.findByFileNameContainingIgnoreCase("doc_1");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long queryCount = getStatistics().getQueryExecutionCount();
        logResults("findByFileNameContainingIgnoreCase", documents.size(), queryCount, duration);

        assertThat(queryCount).isLessThanOrEqualTo(1);
        // Должно найти doc_10, doc_11, doc_12, doc_13, doc_14, doc_15, doc_16, doc_17, doc_18, doc_19
        assertThat(documents).hasSize(11); // doc_10 - doc_19 (10 штук) + doc_1 (1 штука) = 11
    }
}