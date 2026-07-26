package com.example.rag.service;

import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для обработки вопросов с использованием RAG (Retrieval-Augmented Generation).
 *
 * <p>Основные функции:
 * <ul>
 *   <li>Прием вопросов от пользователя</li>
 *   <li>Поиск релевантных фрагментов в векторной базе данных</li>
 *   <li>Формирование контекстного промпта с системными инструкциями</li>
 *   <li>Генерация ответа с помощью LLM (Ollama)</li>
 * </ul>
 * </p>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    /**
     * Клиент для взаимодействия с чат-моделью.
     */
    private final ChatClient chatClient;

    /**
     * Хранилище векторов для поиска релевантных фрагментов документов.
     */
    private final VectorStore vectorStore;

    /**
     * Обрабатывает вопрос пользователя с использованием RAG.
     *
     * @param question вопрос пользователя
     * @return ответ на вопрос
     */
    @Transactional
    public String ask(String question) {
        log.info("❓ Вопрос: {}", question);

        try {
            // ============================================================
            // 0. ВАЛИДАЦИЯ ВХОДНЫХ ДАННЫХ
            // ============================================================
            if (question == null || question.trim().isEmpty()) {
                log.warn("⚠️ Получен пустой вопрос");
                return "Пожалуйста, задайте вопрос.";
            }

            if (chatClient == null) {
                log.warn("⚠️ ChatClient не инициализирован");
                return "Извините, сервис временно недоступен. Пожалуйста, попробуйте позже.";
            }

            if (vectorStore == null) {
                log.warn("⚠️ VectorStore не инициализирован");
                return "Извините, система временно недоступна. Пожалуйста, попробуйте позже.";
            }

            // ============================================================
            // 1. СИСТЕМНЫЙ ПРОМПТ
            // ============================================================
            String systemPrompt = """
                    Ты — умный ассистент, который отвечает на вопросы на основе предоставленных документов.
                    
                    ВАЖНЫЕ ПРАВИЛА:
                    1. Отвечай ТОЛЬКО на русском языке
                    2. Если пользователь спрашивает на русском — отвечай на русском
                    3. Используй информацию из документов для ответа
                    4. Если информации в документах нет — скажи об этом честно
                    5. Будь вежливым и профессиональным
                    6. Структурируй ответ для удобства чтения (используй списки, абзацы)
                    """;

            // ============================================================
            // 2. ПОИСК В ВЕКТОРНОЙ БАЗЕ
            // ============================================================
            var searchRequest = SearchRequest.builder()
                    .query(question)
                    .topK(5)
                    .similarityThreshold(0.3)
                    .build();

            var documents = vectorStore.similaritySearch(searchRequest);

            // ============================================================
            // 3. ФОРМИРОВАНИЕ КОНТЕКСТА
            // ============================================================
            String context = "";
            if (!documents.isEmpty()) {
                log.info("📚 Найдено {} релевантных документов", documents.size());
                context = documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n\n---\n\n"));
            } else {
                log.warn("⚠️ Релевантные документы не найдены");
            }

            // ============================================================
            // 4. ФОРМИРОВАНИЕ СООБЩЕНИЯ
            // ============================================================
            var userMessage = new UserMessage(
                    context.isEmpty()
                            ? "Вопрос: " + question + "\n\nИнформация в документах не найдена. Ответь на основе своих знаний, но сообщи об этом."
                            : "Контекст из документов:\n" + context + "\n\nНа основе контекста ответь на вопрос: " + question
            );

            // ============================================================
            // 5. ЗАПРОС К LLM
            // ============================================================
            var response = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(userMessage)
                    .call()
                    .content();

            // ============================================================
            // 6. ПРОВЕРКА И ВОЗВРАТ ОТВЕТА
            // ============================================================
            if (response == null || response.trim().isEmpty()) {
                log.warn("⚠️ Ответ пустой");
                return "Извините, я не нашел информации по вашему вопросу в загруженных документах. " +
                       "Пожалуйста, уточните вопрос или загрузите документ.";
            }

            log.info("✅ Ответ получен (длина: {} символов)", response.length());
            return response;

        } catch (Exception exception) {
            log.error("❌ Ошибка при получении ответа", exception);
            return "Извините, произошла ошибка при обработке запроса. Пожалуйста, попробуйте позже.";
        }
    }
}