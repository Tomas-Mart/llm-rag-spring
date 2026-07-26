package com.example.rag.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Контроллер для отображения веб-страниц чата.
 * <p>
 * Отвечает за:
 * <ul>
 *   <li>Отображение главной страницы с формой вопроса</li>
 *   <li>Обработку POST запросов с вопросами</li>
 *   <li>Передачу данных в Thymeleaf шаблоны</li>
 * </ul>
 * <p>
 * Вся бизнес-логика делегируется {@link RagService}.
 *
 * @author RAG Application Team
 * @version 1.0
 * @see RagService
 * @since 1.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private static final String QUESTION_ATTR = "question";
    private static final String ANSWER_ATTR = "answer";
    private static final String VIEW_INDEX = "index";

    private final RagService ragService;

    /**
     * Отображает главную страницу с пустыми полями.
     *
     * @param model модель для передачи данных в шаблон
     * @return имя Thymeleaf шаблона
     */
    @GetMapping("/")
    public String index(Model model) {
        log.debug("📄 Отображение главной страницы");
        model.addAttribute(QUESTION_ATTR, "");
        model.addAttribute(ANSWER_ATTR, "");
        return VIEW_INDEX;
    }

    /**
     * Обрабатывает вопрос пользователя и возвращает ответ.
     * <p>
     * В случае ошибки возвращает сообщение об ошибке в поле answer.
     *
     * @param question текст вопроса
     * @param model    модель для передачи данных в шаблон
     * @return имя Thymeleaf шаблона
     */
    @PostMapping("/ask")
    public String ask(@RequestParam(QUESTION_ATTR) String question, Model model) {
        log.info("📝 Вопрос: {}", question);

        try {
            String answer = ragService.ask(question);
            log.info("✅ Ответ получен, длина: {} символов", answer.length());

            model.addAttribute(QUESTION_ATTR, question);
            model.addAttribute(ANSWER_ATTR, answer);

        } catch (Exception e) {
            log.error("❌ Ошибка при обработке вопроса: {}", e.getMessage(), e);
            model.addAttribute(QUESTION_ATTR, question);
            model.addAttribute(ANSWER_ATTR, "❌ Извините, произошла ошибка: " + e.getMessage());
        }

        return VIEW_INDEX;
    }
}