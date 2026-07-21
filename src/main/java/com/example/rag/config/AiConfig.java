package com.example.rag.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    // === БАЗОВЫЕ БИНЫ (общие для всех профилей) ===

    @Bean
    public OllamaApi ollamaApi() {
        try {
            log.info("🔧 Инициализация Ollama API на http://localhost:11434");
            return OllamaApi.builder()
                    .baseUrl("http://localhost:11434")
                    .build();
        } catch (Exception e) {
            log.error("❌ Не удалось создать OllamaApi", e);
            throw new RuntimeException("Ollama API initialization failed", e);
        }
    }

    @Bean
    public OllamaChatModel chatModel(OllamaApi ollamaApi) {
        try {
            log.info("🔧 Инициализация OllamaChatModel с моделью qwen2.5-coder:7b");
            return OllamaChatModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(OllamaChatOptions.builder()
                            .model("qwen2.5-coder:7b")
                            .temperature(0.2)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("❌ Не удалось создать OllamaChatModel", e);
            throw new RuntimeException("OllamaChatModel initialization failed", e);
        }
    }

    @Bean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        try {
            log.info("🔧 Инициализация ChatClient");
            return ChatClient.builder(chatModel).build();
        } catch (Exception e) {
            log.error("❌ Не удалось создать ChatClient", e);
            throw new RuntimeException("ChatClient initialization failed", e);
        }
    }

    // === ✅ PgVectorStore (постоянное хранилище) ===

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        try {
            log.info("🔧 Инициализация PgVectorStore с таблицей vector_store");
            return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                    .vectorTableName("vector_store")
                    .dimensions(768)
                    .initializeSchema(true)
                    .build();
        } catch (Exception e) {
            log.error("❌ Не удалось создать PgVectorStore", e);
            throw new RuntimeException("PgVectorStore initialization failed", e);
        }
    }

    // === ТЕСТОВЫЙ DataSource (только для профиля test) ===

    @Bean
    @Primary
    @Profile("test")
    public DataSource testDataSource() {
        try {
            log.info("🔧 Инициализация тестового DataSource (H2)");
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        } catch (Exception e) {
            log.error("❌ Не удалось создать тестовый DataSource", e);
            throw new RuntimeException("Test DataSource initialization failed", e);
        }
    }
}