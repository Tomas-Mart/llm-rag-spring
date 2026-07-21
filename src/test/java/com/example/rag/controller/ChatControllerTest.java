package com.example.rag.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import com.example.rag.service.RagService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private RagService ragService;

    @Mock
    private Model model;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
    }

    @Test
    void testIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("question"))
                .andExpect(model().attributeExists("answer"));

        System.out.println("✅ Тест главной страницы пройден");
    }

    @Test
    void testIndexPageWithModelAttributes() {
        String viewName = chatController.index(model);
        assertThat(viewName).isEqualTo("index");
        verify(model).addAttribute("question", "");
        verify(model).addAttribute("answer", "");

        System.out.println("✅ Тест атрибутов модели пройден");
    }

    @Test
    void testAskQuestion() throws Exception {
        String question = "What is Spring AI?";
        String expectedAnswer = "Spring AI is a framework for building AI applications.";
        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param("question", question))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("question", question))
                .andExpect(model().attribute("answer", expectedAnswer));

        verify(ragService).ask(question);

        System.out.println("✅ Тест вопроса пройден");
    }

    @Test
    void testAskQuestionWithEmptyQuestion() throws Exception {
        String question = "";
        String expectedAnswer = "Пожалуйста, задайте вопрос.";
        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param("question", question))
                .andExpect(status().isOk())
                .andExpect(model().attribute("question", question))
                .andExpect(model().attribute("answer", expectedAnswer));

        verify(ragService).ask(question);

        System.out.println("✅ Тест с пустым вопросом пройден");
    }

    @Test
    void testAskQuestionWithLongQuestion() throws Exception {
        String question = "What is the difference between " +
                          "Spring AI and LangChain4j? Which one should I use for " +
                          "building RAG applications with vector databases?";
        String expectedAnswer = "Both frameworks can be used, but Spring AI is more integrated with Spring ecosystem.";
        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param("question", question))
                .andExpect(status().isOk())
                .andExpect(model().attribute("question", question))
                .andExpect(model().attribute("answer", expectedAnswer));

        verify(ragService).ask(question);

        System.out.println("✅ Тест с длинным вопросом пройден");
    }

    @Test
    void testAskQuestionWithSpecialCharacters() throws Exception {
        String question = "Что такое RAG? Spring AI vs LangChain4j? 🚀";
        String expectedAnswer = "RAG - это Retrieval-Augmented Generation.";
        when(ragService.ask(question)).thenReturn(expectedAnswer);

        mockMvc.perform(post("/ask")
                        .param("question", question))
                .andExpect(status().isOk())
                .andExpect(model().attribute("question", question))
                .andExpect(model().attribute("answer", expectedAnswer));

        verify(ragService).ask(question);

        System.out.println("✅ Тест со специальными символами пройден");
    }

    @Test
    void testAskQuestionWhenServiceThrowsException() throws Exception {
        String question = "Test question";
        when(ragService.ask(question)).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/ask")
                        .param("question", question))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("question", question))
                .andExpect(model().attribute("answer", "Извините, произошла ошибка: Service error"));

        verify(ragService).ask(question);

        System.out.println("✅ Тест с ошибкой сервиса пройден");
    }
}