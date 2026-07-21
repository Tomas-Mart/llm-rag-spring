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
import com.example.rag.service.DocumentIngestionService;
import com.example.rag.service.RagService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Абстрактный базовый класс для тестов производительности.
 * Использует моки для изоляции от реальных зависимостей.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public abstract class BasePerformanceTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected DocumentIngestionService ingestionService;

    @MockBean
    protected RagService ragService;

    @MockBean
    protected OllamaApi ollamaApi;

    @MockBean
    protected OllamaChatModel chatModel;

    @MockBean
    protected VectorStore vectorStore;

    @BeforeEach
    void setUpBase() {
        logger.info("🚀 Running performance test: {}", getClass().getSimpleName());
    }

    protected void assertMocksCreated() {
        assertThat(ragService).isNotNull();
        assertThat(ollamaApi).isNotNull();
        assertThat(chatModel).isNotNull();
        assertThat(vectorStore).isNotNull();
        logger.info("All mocks created successfully");
    }
}