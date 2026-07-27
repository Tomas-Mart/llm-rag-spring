package com.example.rag.service;

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
import com.example.rag.service.test.DocumentServiceForTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DataJpaTest
@ActiveProfiles("test")
@Tag("performance")
class DocumentServiceNPlusOneTest {

    @Autowired
    private DocumentServiceForTest documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        // Создаем тестовые данные
        for (int i = 0; i < 20; i++) {
            DocumentEntity doc = new DocumentEntity();
            doc.setFileName("doc_" + i + ".txt");
            doc.setContent("Content " + i);
            doc.setMetadata("{}");
            documentRepository.save(doc);
        }
        entityManager.flush();
        entityManager.clear();

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
        log.info("📊 SERVICE N+1 TEST:");
        log.info("   Documents returned: {}", documents.size());
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        // Должен быть 1 запрос (или 2 если есть связи)
        assertThat(queryCount).isLessThanOrEqualTo(2);
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
        log.info("📊 SERVICE N+1 TEST (findByFileName):");
        log.info("   Document found: {}", document != null);
        log.info("   Queries executed: {}", queryCount);
        log.info("========================================");

        assertThat(queryCount).isLessThanOrEqualTo(1);
        assertThat(document).isNotNull();
        assertThat(document.fileName()).isEqualTo("doc_5.txt");
    }
}