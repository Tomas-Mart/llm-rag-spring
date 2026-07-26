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

    private final RagService ragService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {
        // ✅ Проверка на null запроса
        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неверный формат запроса"));
        }

        String question = request.get("question");

        // ✅ Проверка на null и пустую строку
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Пожалуйста, задайте вопрос."));
        }

        log.info("📝 [API] Вопрос: {}", question);

        try {
            String answer = ragService.ask(question);
            return ResponseEntity.ok(Map.of(
                    "question", question,
                    "answer", answer
            ));
        } catch (Exception e) {
            log.error("❌ [API] Ошибка: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}