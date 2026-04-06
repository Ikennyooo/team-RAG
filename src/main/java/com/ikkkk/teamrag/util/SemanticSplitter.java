package com.ikkkk.teamrag.util;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticSplitter {

    private final EmbeddingModel embeddingModel;
    private final Tokenizer tokenizer;

    @Value("${rag.semantic-splitter.similarity-threshold}")
    private Double similarityThreshold;

    @Value("${rag.semantic-splitter.max-tokens}")
    private Integer maxTokens;

    @Value("${rag.semantic-splitter.overlap-tokens}")
    private Integer overlapTokens;

    public List<TextSegment> split(Document document) {
        String name = document.metadata().getString("name");
        if (name == null || name.isBlank()) {
            name = "unknown";
        }
        log.info("开始对文档[{}]进行语义分块，相似度阈值：{}，最大token：{}", name, similarityThreshold, maxTokens);
        DocumentSplitter baseSplitter = DocumentSplitters.recursive(maxTokens / 4, 0, tokenizer);
        List<TextSegment> baseSegments = baseSplitter.split(document);
        if (baseSegments.size() <= 1) {
            return baseSegments;
        }

        List<Embedding> embeddings = new ArrayList<>();
        for (TextSegment segment : baseSegments) {
            embeddings.add(embeddingModel.embed(segment.text()).content());
        }

        List<TextSegment> semanticSegments = new ArrayList<>();
        StringBuilder currentBlock = new StringBuilder(baseSegments.get(0).text());
        int currentTokenCount = tokenizer.estimateTokenCountInText(currentBlock.toString());

        for (int i = 1; i < baseSegments.size(); i++) {
            TextSegment nextSeg = baseSegments.get(i);
            Embedding currentEmb = embeddings.get(i - 1);
            Embedding nextEmb = embeddings.get(i);

            double similarity = calculateCosineSimilarity(currentEmb.vector(), nextEmb.vector());
            int nextTokenCount = tokenizer.estimateTokenCountInText(nextSeg.text());

            if (similarity >= similarityThreshold && (currentTokenCount + nextTokenCount) <= maxTokens) {
                currentBlock.append("\n").append(nextSeg.text());
                currentTokenCount += nextTokenCount;
            } else {
                semanticSegments.add(TextSegment.from(addOverlap(currentBlock.toString(), nextSeg.text())));
                currentBlock = new StringBuilder(nextSeg.text());
                currentTokenCount = nextTokenCount;
            }
        }

        semanticSegments.add(TextSegment.from(currentBlock.toString()));
        log.info("文档[{}]语义分块完成，原始{}个基础片段，生成{}个语义块", name, baseSegments.size(), semanticSegments.size());
        return semanticSegments;
    }

    private double calculateCosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量维度不一致，无法计算相似度");
        }
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += Math.pow(vec1[i], 2);
            norm2 += Math.pow(vec2[i], 2);
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private String addOverlap(String currentText, String nextText) {
        if (overlapTokens <= 0) {
            return currentText;
        }
        String[] words = nextText.trim().split("\\s+");
        int n = Math.min(overlapTokens, words.length);
        StringBuilder overlap = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                overlap.append(' ');
            }
            overlap.append(words[i]);
        }
        return currentText + "\n" + overlap;
    }
}
