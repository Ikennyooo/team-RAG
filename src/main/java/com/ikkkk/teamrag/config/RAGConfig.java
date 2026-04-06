package com.ikkkk.teamrag.config;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RAGConfig {

    @Value("${langchain4j.community.dashscope.streaming-chat-model.api-key}")
    private String dashScopeApiKey;

    @Value("${langchain4j.community.dashscope.streaming-chat-model.model-name}")
    private String chatModelName;

    @Value("${langchain4j.community.dashscope.streaming-chat-model.temperature}")
    private Float temperature;

    @Bean
    public QwenStreamingChatModel streamingChatModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(dashScopeApiKey)
                .modelName(chatModelName)
                .temperature(temperature)
                .build();
    }
}
