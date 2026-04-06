package com.ikkkk.teamrag.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Value("${langchain4j.community.dashscope.embedding-model.api-key}")
    private String dashScopeApiKey;

    @Value("${langchain4j.community.dashscope.embedding-model.model-name}")
    private String embeddingModelName;

//    @Bean
//    public QwenEmbeddingModel embeddingModel() {
//        return QwenEmbeddingModel.builder()
//                .apiKey(dashScopeApiKey)
//                .modelName(embeddingModelName)
//                .build();
//    }
}
