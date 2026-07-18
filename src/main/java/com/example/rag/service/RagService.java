package com.example.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public String ask(String question) {
        log.info("❓ Вопрос: {}", question);

        // RAG-пайплайн через QuestionAnswerAdvisor
        var advisor = new QuestionAnswerAdvisor(
                vectorStore,
                SearchRequest.query(question)
                        .withTopK(5)
                        .withSimilarityThreshold(0.5)
        );

        // Отправляем запрос с контекстом
        var response = chatClient.prompt()
                .user(question)
                .advisors(advisor)
                .call()
                .content();

        log.info("✅ Ответ получен (длина: {} символов)", response.length());
        return response;
    }
}