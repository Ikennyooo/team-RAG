package com.ikkkk.teamrag.util;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BM25Retriever {

    private final PineconeEmbeddingStore pineconeEmbeddingStore;
    private final JaccardSimilarity jaccard = new JaccardSimilarity();

    @Value("${rag.hybrid-retrieval.bm25-top-k}")
    private Integer bm25TopK;

    public List<TextSegment> retrieve(String query) {
        log.info("开始BM25关键词检索，查询词：{}，返回前{}条", query, bm25TopK);
        List<TextSegment> allSegments = getAllSegmentsFromPinecone();
        if (allSegments.isEmpty()) {
            log.warn("BM25检索：向量库中无文本片段");
            return Collections.emptyList();
        }

        Map<TextSegment, Double> scoreMap = new HashMap<>();
        for (TextSegment segment : allSegments) {
            scoreMap.put(segment, calculateBM25Score(query, segment.text()));
        }

        return scoreMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(bm25TopK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double calculateBM25Score(String query, String text) {
        Set<String> queryWords = splitToWords(query);
        Set<String> textWords = splitToWords(text);
        if (queryWords.isEmpty() || textWords.isEmpty()) {
            return 0.0;
        }

        double jaccardScore = jaccard.apply(query, text);
        int termFreq = 0;
        List<String> textWordList = new ArrayList<>(splitToWords(text));
        for (String word : queryWords) {
            termFreq += Collections.frequency(textWordList, word);
        }
        return jaccardScore * (1 + Math.log1p(termFreq));
    }

    private Set<String> splitToWords(String text) {
        String cleanText = text.replaceAll("[\\pP\\pS]", " ").toLowerCase().trim();
        return new HashSet<>(Arrays.asList(cleanText.split("\\s+")));
    }

    @SuppressWarnings("unused")
    private List<TextSegment> getAllSegmentsFromPinecone() {
        return new ArrayList<>();
    }
}
