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

/**
 * Конфигурация AI компонентов для приложения.
 *
 * <p>Настраивает следующие компоненты:
 * <ul>
 *   <li>Ollama API клиент для взаимодействия с LLM</li>
 *   <li>Ollama Chat Model для генерации ответов</li>
 *   <li>ChatClient для высокоуровневой работы с чатом</li>
 *   <li>PgVectorStore для хранения и поиска эмбеддингов</li>
 *   <li>Тестовый DataSource для профиля test</li>
 * </ul>
 *
 * <p>Важно: в тестовом профиле (test) создаются только необходимые бины,
 * остальные заменяются на моки для изоляции тестов.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    /**
     * Создает клиент для Ollama API.
     * Используется для низкоуровневого взаимодействия с Ollama сервером.
     *
     * <p>Не создается в тестовом профиле, так как заменяется на мок.</p>
     *
     * @return экземпляр {@link OllamaApi}
     */
    @Bean
    @Profile("!test")
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

    /**
     * Создает модель чата для Ollama.
     * Используется для генерации ответов на основе запросов пользователя.
     *
     * <p>Не создается в тестовом профиле, так как заменяется на мок.</p>
     *
     * @param ollamaApi клиент для Ollama API
     * @return экземпляр {@link OllamaChatModel}
     */
    @Bean
    @Profile("!test")
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

    /**
     * Создает высокоуровневый клиент для работы с чатом.
     * Обеспечивает Fluent API для построения запросов к LLM.
     *
     * <p>Не создается в тестовом профиле, так как заменяется на мок.</p>
     *
     * @param chatModel модель чата
     * @return экземпляр {@link ChatClient}
     */
    @Bean
    @Profile("!test")
    public ChatClient chatClient(OllamaChatModel chatModel) {
        try {
            log.info("🔧 Инициализация ChatClient");
            return ChatClient.builder(chatModel).build();
        } catch (Exception e) {
            log.error("❌ Не удалось создать ChatClient", e);
            throw new RuntimeException("ChatClient initialization failed", e);
        }
    }

    /**
     * Создает векторное хранилище на основе pgvector.
     * Используется для хранения и поиска эмбеддингов документов.
     *
     * <p>Не создается в тестовом профиле, так как заменяется на мок.</p>
     *
     * @param jdbcTemplate   шаблон JDBC для работы с базой данных
     * @param embeddingModel модель для создания эмбеддингов
     * @return экземпляр {@link VectorStore}
     */
    @Bean
    @Profile("!test")
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

    /**
     * Создает DataSource для тестового профиля.
     * Использует H2 в памяти с режимом совместимости с PostgreSQL.
     *
     * <p>Создается только в тестовом профиле и помечен как {@link Primary},
     * чтобы переопределить основной DataSource.</p>
     *
     * @return экземпляр {@link DataSource} для H2
     */
    @Bean
    @Primary
    @Profile("test")
    public DataSource testDataSource() {
        try {
            log.info("🔧 Инициализация тестового DataSource (H2)");
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        } catch (Exception e) {
            log.error("❌ Не удалось создать тестовый DataSource", e);
            throw new RuntimeException("Test DataSource initialization failed", e);
        }
    }
}