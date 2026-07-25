package com.example.rag.config;

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
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Конфигурация AI компонентов для приложения.
 *
 * <p>Настраивает следующие компоненты:
 * <ul>
 *   <li>Ollama API клиент для взаимодействия с LLM</li>
 *   <li>Ollama Chat Model для генерации ответов</li>
 *   <li>ChatClient для высокоуровневой работы с чатом</li>
 *   <li>PgVectorStore для хранения и поиска эмбеддингов</li>
 * </ul>
 *
 * <p>Важно: В тестовом профиле (test) используются моки из {@code BaseTest}.
 * Все бины с аннотацией {@code @Profile("!test")} создаются только вне тестового профиля.
 *
 * @author RAG Application Team
 * @version 2.0
 * @since 1.0
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    // ============================================================
    // OLLAMA КОМПОНЕНТЫ
    // ============================================================

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
        log.info("🔧 Initializing Ollama API on http://localhost:11434");
        return OllamaApi.builder()
                .baseUrl("http://localhost:11434")
                .build();
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
        log.info("🔧 Initializing OllamaChatModel with model qwen2.5-coder:7b");
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model("qwen2.5-coder:7b")
                        .temperature(0.2)
                        .build())
                .build();
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
        log.info("🔧 Initializing ChatClient");
        return ChatClient.builder(chatModel).build();
    }

    // ============================================================
    // ВЕКТОРНОЕ ХРАНИЛИЩЕ
    // ============================================================

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
        log.info("🔧 Initializing PgVectorStore with table vector_store");
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("vector_store")
                .dimensions(768)
                .initializeSchema(true)
                .build();
    }
}