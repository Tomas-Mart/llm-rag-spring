package com.example.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public OllamaApi ollamaApi() {
        return new OllamaApi("http://localhost:11434");
    }

    @Bean
    public OllamaChatModel chatModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .withOllamaApi(ollamaApi)
                .withDefaultOptions(OllamaOptions.create()
                        .withModel("qwen2.5-coder:7b")
                        .withTemperature(0.2))
                .build();
    }

    @Bean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}