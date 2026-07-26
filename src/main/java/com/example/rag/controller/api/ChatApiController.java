package com.example.rag.controller.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatApiController {

    private static final String ERROR_KEY = "error";
    private static final String QUESTION_KEY = "question";
    private static final String ANSWER_KEY = "answer";

    private final RagService ragService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {
        // ✅ Проверка на null запроса
        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Неверный формат запроса"));
        }

        String question = request.get(QUESTION_KEY);

        // ✅ Проверка на null и пустую строку
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Пожалуйста, задайте вопрос."));
        }

        log.info("📝 [API] Вопрос: {}", question);

        try {
            String answer = ragService.ask(question);
            return ResponseEntity.ok(Map.of(
                    QUESTION_KEY, question,
                    ANSWER_KEY, answer
            ));
        } catch (Exception e) {
            log.error("❌ [API] Ошибка: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }
}