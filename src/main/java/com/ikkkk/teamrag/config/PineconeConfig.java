package com.ikkkk.teamrag.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PineconeConfig {

    @Value("${pinecone.api-key}")
    private String pineconeApiKey;

    @Value("${pinecone.index}")
    private String pineconeIndex;

    @Value("${pinecone.region}")
    private String pineconeRegion;

    @Value("${pinecone.namespace}")
    private String pineconeNamespace;

    @Bean
    public PineconeEmbeddingStore pineconeEmbeddingStore(EmbeddingModel embeddingModel) {
        int dimension = embeddingModel.dimension();
        return PineconeEmbeddingStore.builder()
                .apiKey(pineconeApiKey)
                .index(pineconeIndex)
                .nameSpace(pineconeNamespace)
                .createIndex(PineconeServerlessIndexConfig.builder()
                        .cloud("AWS")
                        .region(pineconeRegion)
                        .dimension(dimension)
                        .build())
                .build();
    }
}
