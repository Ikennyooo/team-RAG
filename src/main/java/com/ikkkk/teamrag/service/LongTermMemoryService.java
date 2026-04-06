package com.ikkkk.teamrag.service;

import com.ikkkk.teamrag.bean.LongTermMemoryRequest;
import com.ikkkk.teamrag.bean.LongTermMemoryResponse;
import com.ikkkk.teamrag.constant.MemoryMetadataKeys;
import com.ikkkk.teamrag.util.SemanticSplitter;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LongTermMemoryService {

    private final SemanticSplitter semanticSplitter;
    private final EmbeddingModel embeddingModel;
    private final PineconeEmbeddingStore pineconeEmbeddingStore;

    /**
     * 将长期记忆文本切块、向量化并写入 Pinecone，供 RAG 问答中的向量检索召回。
     */
    public LongTermMemoryResponse ingest(LongTermMemoryRequest request) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("content 不能为空");
        }

        String memoryRecordId = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();
        String name = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle()
                : "long-term-" + memoryRecordId;

        Document document = Document.from(request.getContent());
        document.metadata().put("name", name);
        document.metadata().put("type", "long_term_memory");

        List<TextSegment> splitSegments = semanticSplitter.split(document);
        List<TextSegment> segmentsWithMeta = attachMetadata(splitSegments, memoryRecordId, createdAt, request);

        List<Embedding> embeddings = embeddingModel.embedAll(segmentsWithMeta).content();
        pineconeEmbeddingStore.addAll(embeddings, segmentsWithMeta);

        log.info("长期记忆入库完成，recordId={}，块数={}", memoryRecordId, segmentsWithMeta.size());
        return new LongTermMemoryResponse(memoryRecordId, segmentsWithMeta.size());
    }

    private List<TextSegment> attachMetadata(
            List<TextSegment> splitSegments,
            String memoryRecordId,
            String createdAt,
            LongTermMemoryRequest request) {

        List<TextSegment> result = new ArrayList<>(splitSegments.size());
        for (int i = 0; i < splitSegments.size(); i++) {
            Metadata md = new Metadata();
            md.put(MemoryMetadataKeys.SOURCE, MemoryMetadataKeys.SOURCE_LONG_TERM_MEMORY);
            md.put("memoryRecordId", memoryRecordId);
            md.put("createdAt", createdAt);
            md.put("segmentIndex", i);
            if (request.getTitle() != null && !request.getTitle().isBlank()) {
                md.put("title", request.getTitle());
            }
            if (request.getClientMemoryId() != null && !request.getClientMemoryId().isBlank()) {
                md.put("clientMemoryId", request.getClientMemoryId());
            }
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                md.put("tags", String.join(",", request.getTags()));
            }
            result.add(TextSegment.from(splitSegments.get(i).text(), md));
        }
        return result;
    }
}
