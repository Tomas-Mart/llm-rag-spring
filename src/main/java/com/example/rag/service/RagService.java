package com.example.rag.service;

import java.util.List;
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
 * <p>Процесс работы:
 * <ol>
 *   <li>Получение вопроса от пользователя</li>
 *   <li>Поиск топ-5 наиболее релевантных чанков в векторной БД</li>
 *   <li>Формирование системного промпта с правилами ответа на русском языке</li>
 *   <li>Отправка запроса к LLM с контекстом из документов</li>
 *   <li>Возврат сгенерированного ответа</li>
 * </ol>
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
     * Используется для отправки промптов и получения ответов от LLM.
     */
    private final ChatClient chatClient;

    /**
     * Хранилище векторов для поиска релевантных фрагментов документов.
     * Содержит эмбеддинги всех загруженных документов.
     */
    private final VectorStore vectorStore;

    /**
     * Обрабатывает вопрос пользователя с использованием RAG (Retrieval-Augmented Generation).
     *
     * <p>Процесс включает:
     * <ol>
     *   <li>Поиск релевантных фрагментов в векторной БД</li>
     *   <li>Формирование промпта с контекстом</li>
     *   <li>Генерацию ответа с помощью LLM</li>
     * </ol>
     * </p>
     *
     * <p>Алгоритм работы:
     * <ul>
     *   <li>Создается запрос на поиск с параметрами: топ-5 чанков, порог схожести 0.3</li>
     *   <li>Формируется системный промпт с правилами ответа на русском языке</li>
     *   <li>Вопрос пользователя дополняется найденным контекстом из документов</li>
     *   <li>LLM генерирует ответ на основе предоставленного контекста</li>
     *   <li>При отсутствии релевантной информации возвращается соответствующее сообщение</li>
     * </ul>
     * </p>
     *
     * @param question вопрос пользователя
     * @return ответ на вопрос, сгенерированный LLM на основе документов
     */
    @Transactional
    public String ask(String question) {
        // Логирование полученного вопроса
        log.info("❓ Вопрос: {}", question);

        try {
            // ============================================================
            // 1. СИСТЕМНЫЙ ПРОМПТ ДЛЯ ОТВЕТОВ НА РУССКОМ ЯЗЫКЕ
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
            // 2. ПОИСК РЕЛЕВАНТНЫХ ФРАГМЕНТОВ В ВЕКТОРНОЙ БАЗЕ ДАННЫХ
            // ============================================================
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .topK(5)
                    .similarityThreshold(0.3)
                    .build();

            // Выполняем поиск в векторной БД
            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            // ============================================================
            // 3. ФОРМИРОВАНИЕ КОНТЕКСТА ИЗ НАЙДЕННЫХ ДОКУМЕНТОВ
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
            // 4. ФОРМИРОВАНИЕ ПОЛЬЗОВАТЕЛЬСКОГО СООБЩЕНИЯ С КОНТЕКСТОМ
            // ============================================================
            UserMessage userMessage;
            if (context.isEmpty()) {
                userMessage = new UserMessage("Вопрос: " + question + "\n\n" +
                                              "Информация в документах не найдена. Ответь на основе своих знаний, но сообщи об этом.");
            } else {
                userMessage = new UserMessage("Контекст из документов:\n" + context + "\n\n" +
                                              "На основе контекста ответь на вопрос: " + question);
            }

            // ============================================================
            // 5. ОТПРАВКА ЗАПРОСА К LLM
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