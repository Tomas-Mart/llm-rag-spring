package com.example.rag.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.rag.service.RagService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;
    private static final String QUESTION = "question";
    private static final String ANSWER = "answer";

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute(QUESTION, "");
        model.addAttribute(ANSWER, "");
        return "index";
    }

    @PostMapping("/ask")
    public String ask(@RequestParam(QUESTION) String question, Model model) {
        try {
            String answer = ragService.ask(question);
            model.addAttribute(QUESTION, question);
            model.addAttribute(ANSWER, answer);
        } catch (Exception e) {
            model.addAttribute(QUESTION, question);
            model.addAttribute(ANSWER, "Извините, произошла ошибка: " + e.getMessage());
        }
        return "index";
    }
}