package com.example.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.service.test.DocumentServiceForTest;
import io.qameta.allure.Feature;

import static org.mockito.Mockito.mock;

/**
 * Тестовая конфигурация для модульных тестов.
 *
 * <h2>Назначение</h2>
 * <p>Создает моки для всех AI компонентов для изоляции тестов
 * от реальных внешних сервисов.</p>
 *
 * <h2>Использование</h2>
 * <p>Активируется только при активном профиле {@code "test"}.</p>
 *
 * <h2>Моки</h2>
 * <ul>
 *   <li>{@link OllamaApi} - клиент для Ollama API</li>
 *   <li>{@link OllamaChatModel} - модель чата</li>
 *   <li>{@link VectorStore} - векторное хранилище</li>
 *   <li>{@link ChatClient} - клиент для чата</li>
 *   <li>{@link EmbeddingModel} - модель эмбеддингов</li>
 * </ul>
 *
 * <h2>Тестовые сервисы</h2>
 * <ul>
 *   <li>{@link DocumentServiceForTest} - сервис для тестирования N+1 проблемы</li>
 * </ul>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * // Конфигурация автоматически применяется в тестовом профиле
 * @SpringBootTest
 * @ActiveProfiles("test")
 * class MyServiceTest {
 *     // Использует моки из TestConfig
 * }
 * }</pre>
 *
 * @author RAG Application Team
 * @version 6.0
 * @since 1.0
 */
@Configuration
@Profile("test")
@Feature("Тестовая конфигурация")
public class TestConfig {

    // ============================================================
    // МОКИ ДЛЯ AI КОМПОНЕНТОВ
    // ============================================================

    /**
     * Создает мок для {@link OllamaApi}.
     *
     * @return мок {@link OllamaApi}
     */
    @Bean
    @Primary
    public OllamaApi mockOllamaApi() {
        return mock(OllamaApi.class);
    }

    /**
     * Создает мок для {@link OllamaChatModel}.
     *
     * @return мок {@link OllamaChatModel}
     */
    @Bean
    @Primary
    public OllamaChatModel mockOllamaChatModel() {
        return mock(OllamaChatModel.class);
    }

    /**
     * Создает мок для {@link VectorStore}.
     *
     * @return мок {@link VectorStore}
     */
    @Bean
    @Primary
    public VectorStore mockVectorStore() {
        return mock(VectorStore.class);
    }

    /**
     * Создает мок для {@link ChatClient}.
     *
     * @param ollamaChatModel мок {@link OllamaChatModel}
     * @return мок {@link ChatClient}
     */
    @Bean
    @Primary
    public ChatClient mockChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    /**
     * Создает мок для {@link EmbeddingModel}.
     *
     * <p>Необходим для удовлетворения зависимости {@link VectorStore}.</p>
     *
     * @return мок {@link EmbeddingModel}
     */
    @Bean
    @Primary
    public EmbeddingModel mockEmbeddingModel() {
        return mock(EmbeddingModel.class);
    }

    // ============================================================
    // ТЕСТОВЫЕ СЕРВИСЫ
    // ============================================================

    /**
     * Создает бин {@link DocumentServiceForTest} для тестирования N+1 проблемы.
     *
     * <p>Этот сервис используется только в тестах для проверки
     * производительности запросов и обнаружения N+1 проблемы.</p>
     *
     * @param documentRepository репозиторий документов
     * @return экземпляр {@link DocumentServiceForTest}
     */
    @Bean
    public DocumentServiceForTest documentServiceForTest(DocumentRepository documentRepository) {
        return new DocumentServiceForTest(documentRepository);
    }
}