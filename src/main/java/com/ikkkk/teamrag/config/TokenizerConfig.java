package com.ikkkk.teamrag.config;

import dev.langchain4j.community.model.dashscope.QwenTokenizer;
import dev.langchain4j.model.Tokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenizerConfig {

    @Bean
    public Tokenizer tokenizer(
            @Value("${langchain4j.community.dashscope.streaming-chat-model.api-key}") String apiKey,
            @Value("${langchain4j.community.dashscope.streaming-chat-model.model-name}") String modelName) {
        return QwenTokenizer.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
