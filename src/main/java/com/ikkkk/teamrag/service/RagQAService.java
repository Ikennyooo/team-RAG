package com.ikkkk.teamrag.service;

import com.ikkkk.teamrag.constant.MemoryMetadataKeys;
import com.ikkkk.teamrag.util.BM25Retriever;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagQAService {

    private final BM25Retriever bm25Retriever;
    private final EmbeddingModel embeddingModel;
    private final PineconeEmbeddingStore pineconeEmbeddingStore;
    private final StreamingChatLanguageModel streamingChatModel;

    @Value("${rag.hybrid-retrieval.vector-top-k}")
    private Integer vectorTopK;

    @Value("${rag.vector-retrieval.min-score}")
    private Double vectorMinScore;

    @Value("${rag.hybrid-retrieval.final-top-k}")
    private Integer finalTopK;

    public Flux<String> qa(String query, Long memoryId) {
        log.info("开始RAG混合检索问答，查询词：{}，对话ID：{}", query, memoryId);
        List<TextSegment> bm25Segments = bm25Retriever.retrieve(query);
        List<TextSegment> vectorSegments = vectorRetrieve(query);
        List<TextSegment> finalSegments = mergeSegments(bm25Segments, vectorSegments);

        if (finalSegments.isEmpty()) {
            log.warn("混合检索未匹配到任何相关片段，直接调用大模型回答");
            return streamChat("请回答问题：" + query);
        }

        String prompt = buildPrompt(query, finalSegments);
        log.info("生成大模型提示词，拼接{}个相关片段", finalSegments.size());
        return streamChat(prompt);
    }

    private Flux<String> streamChat(String prompt) {
        return Flux.create(sink -> streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.next(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                sink.complete();
            }

            @Override
            public void onError(Throwable error) {
                sink.error(error);
            }
        }));
    }

    private List<TextSegment> vectorRetrieve(String query) {
        log.info("开始向量语义检索，查询词：{}，返回前{}条，最小相似度：{}", query, vectorTopK, vectorMinScore);
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(vectorTopK)
                .minScore(vectorMinScore)
                .build();
        return pineconeEmbeddingStore.search(request).matches().stream()
                .map(m -> m.embedded())
                .collect(Collectors.toList());
    }

    private List<TextSegment> mergeSegments(List<TextSegment> bm25, List<TextSegment> vector) {
        List<TextSegment> all = new ArrayList<>(bm25);
        for (TextSegment v : vector) {
            boolean dup = all.stream().anyMatch(a -> a.text().equals(v.text()));
            if (!dup) {
                all.add(v);
            }
        }
        return all.stream().limit(finalTopK).collect(Collectors.toList());
    }

    private String buildPrompt(String query, List<TextSegment> segments) {
        StringBuilder context = new StringBuilder();
        context.append("以下是相关的团队知识库内容，请基于这些内容回答问题，若内容中无答案，请说明“知识库中未找到相关答案”，不要编造：\n");
        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);
            String label = segmentLabel(seg);
            context.append(String.format("%d、%s%s\n", i + 1, label, seg.text()));
        }
        context.append("\n用户问题：").append(query);
        return context.toString();
    }

    /**
     * 根据 metadata 区分长期记忆与普通知识库片段，便于模型理解上下文来源。
     */
    private String segmentLabel(TextSegment segment) {
        String source = segment.metadata().getString(MemoryMetadataKeys.SOURCE);
        if (Objects.equals(MemoryMetadataKeys.SOURCE_LONG_TERM_MEMORY, source)) {
            return "【长期记忆】";
        }
        return "";
    }
}
