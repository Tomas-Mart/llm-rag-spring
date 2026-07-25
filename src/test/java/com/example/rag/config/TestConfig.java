package com.example.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * Тестовая конфигурация для модульных тестов.
 * Используется только при активном профиле "test".
 *
 * <p>Создает моки для всех AI компонентов, чтобы изолировать
 * тесты от реальных внешних сервисов.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
@Profile("test")
public class TestConfig {

    /**
     * Мок для Ollama API.
     * Используется для изоляции от реального Ollama сервера.
     */
    @Bean
    @Primary
    public OllamaApi mockOllamaApi() {
        return mock(OllamaApi.class);
    }

    /**
     * Мок для Ollama Chat Model.
     * Используется для изоляции от реальной LLM.
     */
    @Bean
    @Primary
    public OllamaChatModel mockOllamaChatModel() {
        return mock(OllamaChatModel.class);
    }

    /**
     * Мок для VectorStore.
     * Используется для изоляции от реальной векторной БД.
     */
    @Bean
    @Primary
    public VectorStore mockVectorStore() {
        return mock(VectorStore.class);
    }

    /**
     * Мок для ChatClient.
     * Использует мок OllamaChatModel.
     */
    @Bean
    @Primary
    public ChatClient mockChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }
}