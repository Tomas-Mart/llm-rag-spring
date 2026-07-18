package com.example.rag.controller;

import com.example.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("question", "");
        model.addAttribute("answer", "");
        return "index";
    }

    @PostMapping("/ask")
    public String ask(@RequestParam("question") String question, Model model) {
        String answer = ragService.ask(question);
        model.addAttribute("question", question);
        model.addAttribute("answer", answer);
        return "index";
    }
}