package com.example.rag.repository;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DataJpaTest
@ActiveProfiles("nplusone")
@Tag("performance")
class DocumentRepositoryNPlusOneTest {

    @Autowired
    private DocumentRepository documentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        // Включаем сбор статистики
        entityManager.unwrap(org.hibernate.Session.class)
                .getSessionFactory()
                .getStatistics()
                .setStatisticsEnabled(true);

        // Очищаем данные
        documentRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        statistics = entityManager.unwrap(org.hibernate.Session.class)
                .getSessionFactory()
                .getStatistics();
    }

    private void createTestDocuments(int count) {
        for (int i = 0; i < count; i++) {
            DocumentEntity doc = new DocumentEntity();
            doc.setFileName("document_" + i + ".txt");
            doc.setContent("Content " + i);
            doc.setMetadata("{\"index\": " + i + "}");
            documentRepository.save(doc);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldNotHaveNPlusOneProblemWhenFetchingAllDocuments() {
        // Given
        createTestDocuments(10);
        statistics.clear();

        // When
        List<DocumentEntity> documents = documentRepository.findAll();

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 findAll:");
        log.info("   Documents: {}", documents.size());
        log.info("   Queries: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(documents).hasSize(10);
    }

    @Test
    void shouldNotHaveNPlusOneProblemWhenFindingByFileName() {
        // Given
        createTestDocuments(30);
        statistics.clear();

        // When
        Optional<DocumentEntity> doc = documentRepository.findByFileName("document_15.txt");

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 findByFileName:");
        log.info("   Found: {}", doc.isPresent());
        log.info("   Queries: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(doc).isPresent();
        assertThat(doc.get().getFileName()).isEqualTo("document_15.txt");
    }

    @Test
    void shouldUseOptimizedQueryForExists() {
        // Given
        createTestDocuments(30);
        statistics.clear();

        // When
        boolean exists = documentRepository.existsByFileNameOptimized("document_15.txt");

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 existsByFileNameOptimized:");
        log.info("   Exists: {}", exists);
        log.info("   Queries: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(exists).isTrue();
    }

    @Test
    void shouldUseOptimizedQueryForIds() {
        // Given
        createTestDocuments(30);
        statistics.clear();

        // When
        List<Long> ids = documentRepository.findAllIds();

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 findAllIds:");
        log.info("   IDs count: {}", ids.size());
        log.info("   Queries: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(ids).hasSize(30);
    }

    @Test
    void shouldMaintainConstantQueriesWithGrowingDataSize() {
        // Given
        int[] sizes = {5, 10, 20, 50};

        for (int size : sizes) {
            documentRepository.deleteAll();
            entityManager.flush();
            entityManager.clear();

            createTestDocuments(size);
            statistics.clear();

            // When
            List<DocumentEntity> documents = documentRepository.findAll();
            long queryCount = statistics.getQueryExecutionCount();

            // Then
            log.info("   Size: {} -> Queries: {}", size, queryCount);

            assertThat(queryCount).isLessThanOrEqualTo(1);
            assertThat(documents).hasSize(size);
        }
    }

    @Test
    void shouldHandleEmptyDatabase() {
        // Given
        documentRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        // When
        List<DocumentEntity> documents = documentRepository.findAll();

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 findAll (empty DB):");
        log.info("   Documents: {}", documents.size());
        log.info("   Queries: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(documents).isEmpty();
    }

    @Test
    void shouldHandleNotFound() {
        // Given
        createTestDocuments(10);
        statistics.clear();

        // When
        Optional<DocumentEntity> doc = documentRepository.findByFileName("nonexistent.txt");

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 findByFileName (not found):");
        log.info("   Found: {}", doc.isPresent());
        log.info("   Queries: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(doc).isEmpty();
    }
}