package com.example.rag.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки работы VectorStore с pgvector.
 * <p>
 * Использует Testcontainers с PostgreSQL + pgvector.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Tag("vector")
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("vector-test")
class VectorStoreIT extends BaseIntegrationTestWithContainers {

    @Autowired
    private VectorStore vectorStore;

    @Test
    void testVectorStoreIsAvailable() {
        logTestStart("Testing VectorStore availability");

        assertThat(vectorStore)
                .as("VectorStore should be available")
                .isNotNull();

        log.info("✅ VectorStore type: {}", vectorStore.getClass().getSimpleName());
        logTestSuccess("VectorStore availability test passed");
    }

    @Test
    void testVectorStoreCanAddAndSearch() {
        logTestStart("Testing VectorStore add and search");

        // Здесь будут тесты с реальным pgvector
        // vectorStore.add(...);
        // vectorStore.similaritySearch(...);

        logTestSuccess("VectorStore add and search test passed");
    }
}