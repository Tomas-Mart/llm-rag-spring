package com.example.rag.nplusone;

import java.util.List;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.rag.dto.DocumentDto;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.repository.DocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DataJpaTest
@ActiveProfiles("nplusone")
@Tag("performance")
class DocumentServiceNPlusOneTest {

    @Autowired
    private DocumentRepository documentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    private DocumentServiceForTest documentService;

    private Long existingDocumentId;

    @BeforeEach
    void setUp() {
        // Создаем сервис вручную
        documentService = new DocumentServiceForTest(documentRepository);

        // Очищаем данные перед каждым тестом
        documentRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Создаем тестовые данные
        for (int i = 0; i < 20; i++) {
            DocumentEntity document = new DocumentEntity();
            document.setFileName("doc_" + i + ".txt");
            document.setContent("Content " + i);
            document.setMetadata("{}");
            documentRepository.save(document);
        }
        entityManager.flush();
        entityManager.clear();

        // Сохраняем ID существующего документа для тестов
        existingDocumentId = documentRepository.findByFileName("doc_5.txt")
                .map(DocumentEntity::getId)
                .orElse(null);

        // Включаем статистику Hibernate
        entityManager.unwrap(org.hibernate.Session.class)
                .getSessionFactory()
                .getStatistics()
                .setStatisticsEnabled(true);
        statistics = entityManager.unwrap(org.hibernate.Session.class)
                .getSessionFactory()
                .getStatistics();
        statistics.clear();
    }

    @Test
    void testGetAllDocumentsDoesNotCauseNPlusOne() {
        // When
        List<DocumentDto> documents = documentService.getAllDocuments();

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (getAllDocuments):");
        log.info("   Documents returned: {}", documents.size());
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(documents).hasSize(20);
    }

    @Test
    void testGetDocumentByFileNameDoesNotCauseNPlusOne() {
        // Given
        statistics.clear();

        // When
        DocumentDto document = documentService.getDocumentByFileName("doc_5.txt");

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (getDocumentByFileName):");
        log.info("   Document found: {}", document != null);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(document).isNotNull();
        assertThat(document.fileName()).isEqualTo("doc_5.txt");
        assertThat(document.content()).isEqualTo("Content 5");
    }

    @Test
    void testGetAllDocumentsOptimizedDoesNotCauseNPlusOne() {
        // When
        List<DocumentDto> documents = documentService.getAllDocumentsOptimized();

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (getAllDocumentsOptimized):");
        log.info("   Documents returned: {}", documents.size());
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(documents).hasSize(20);
    }

    @Test
    void testGetAllDocumentIdsDoesNotCauseNPlusOne() {
        // Given
        statistics.clear();

        // When
        List<Long> identifiers = documentService.getAllDocumentIds();

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (getAllDocumentIds):");
        log.info("   IDs count: {}", identifiers.size());
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(identifiers).hasSize(20);
    }

    @Test
    void testExistsByFileNameDoesNotCauseNPlusOne() {
        // Given
        statistics.clear();

        // When
        boolean exists = documentService.existsByFileName("doc_5.txt");

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (existsByFileName):");
        log.info("   Exists: {}", exists);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByFileNameNotFound() {
        // Given
        statistics.clear();

        // When
        boolean exists = documentService.existsByFileName("nonexistent.txt");

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (existsByFileName - not found):");
        log.info("   Exists: {}", exists);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(exists).isFalse();
    }

    @Test
    void testGetDocumentByFileNameNotFound() {
        // Given
        statistics.clear();

        // When
        DocumentDto document = documentService.getDocumentByFileName("nonexistent.txt");

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (getDocumentByFileName - not found):");
        log.info("   Document found: {}", document != null);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(document).isNull();
    }

    @Test
    void testDocumentDtoWithContentUpdate() {
        // Given
        DocumentDto original = documentService.getDocumentByFileName("doc_5.txt");

        // When
        DocumentDto updated = original.withContent("Updated content");

        // Then
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.fileName()).isEqualTo(original.fileName());
        assertThat(updated.content()).isEqualTo("Updated content");
        assertThat(updated.metadata()).isEqualTo(original.metadata());

        // Оригинал не изменился (иммутабельность)
        assertThat(original.content()).isNotEqualTo(updated.content());

        log.info("✅ DocumentDto immutability test passed");
    }

    @Test
    void testGetDocumentByIdDoesNotCauseNPlusOne() {
        // Given
        statistics.clear();

        // Проверяем, что ID существует
        assertThat(existingDocumentId)
                .as("Document with ID should exist")
                .isNotNull();

        // When
        DocumentDto document = documentService.getDocumentById(existingDocumentId);

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (getDocumentById):");
        log.info("   Document found: {}", document != null);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(document).isNotNull();
        assertThat(document.id()).isEqualTo(existingDocumentId);
        assertThat(document.fileName()).isEqualTo("doc_5.txt");
    }

    @Test
    void testCountAllDocumentsDoesNotCauseNPlusOne() {
        // Given
        statistics.clear();

        // When
        long count = documentService.countAllDocuments();

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (countAllDocuments):");
        log.info("   Total documents: {}", count);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(count).isEqualTo(20);
    }

    @Test
    void testIsEmptyWhenDatabaseIsNotEmpty() {
        // When
        boolean empty = documentService.isEmpty();

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (isEmpty - not empty):");
        log.info("   Is empty: {}", empty);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(empty).isFalse();
    }

    @Test
    void testExistsByIdDoesNotCauseNPlusOne() {
        // Given
        statistics.clear();

        // Проверяем, что ID существует
        assertThat(existingDocumentId)
                .as("Document with ID should exist")
                .isNotNull();

        // When
        boolean exists = documentService.existsById(existingDocumentId);

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (existsById):");
        log.info("   Exists: {}", exists);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByIdNotFound() {
        // Given
        statistics.clear();

        // Используем заведомо несуществующий ID
        Long nonExistentId = 999L;

        // When
        boolean exists = documentService.existsById(nonExistentId);

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (existsById - not found):");
        log.info("   Exists: {}", exists);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(exists).isFalse();
    }

    @Test
    void testGetTopDocumentsDoesNotCauseNPlusOne() {
        // Given
        statistics.clear();

        // When
        List<DocumentDto> documents = documentService.getTopDocuments(5);

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (getTopDocuments):");
        log.info("   Documents returned: {}", documents.size());
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(documents).hasSize(5);
    }

    @Test
    void testGetDocumentsSortedByFileNameDoesNotCauseNPlusOne() {
        // Given
        statistics.clear();

        // When
        List<DocumentDto> documents = documentService.getDocumentsSortedByFileName();

        // Then
        long queryCount = statistics.getQueryExecutionCount();

        log.info("========================================");
        log.info("📊 SERVICE N+1 TEST (getDocumentsSortedByFileName):");
        log.info("   Documents returned: {}", documents.size());
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(documents).hasSize(20);
        // Проверяем сортировку
        for (int i = 0; i < documents.size() - 1; i++) {
            assertThat(documents.get(i).fileName())
                    .isLessThan(documents.get(i + 1).fileName());
        }
    }
}