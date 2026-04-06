package com.ikkkk.teamrag.service;

import com.ikkkk.teamrag.util.SemanticSplitter;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final SemanticSplitter semanticSplitter;
    private final EmbeddingModel embeddingModel;
    private final PineconeEmbeddingStore pineconeEmbeddingStore;

    public String uploadAndStore(MultipartFile file, String docType) {
        try {
            if (file == null || file.isEmpty()) {
                return "文档文件不能为空";
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return "文档文件名无效";
            }
            log.info("开始处理文档：{}，类型：{}", originalFilename, docType);

            String suffix = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : "";
            File tempFile = File.createTempFile(UUID.randomUUID().toString(), suffix);
            file.transferTo(tempFile);

            Document document;
            if (originalFilename.toLowerCase().endsWith(".pdf")) {
                document = FileSystemDocumentLoader.loadDocument(tempFile.getAbsolutePath(), new ApachePdfBoxDocumentParser());
            } else if (originalFilename.toLowerCase().endsWith(".doc") || originalFilename.toLowerCase().endsWith(".docx")) {
                document = FileSystemDocumentLoader.loadDocument(tempFile.getAbsolutePath(), new ApachePoiDocumentParser());
            } else {
                document = FileSystemDocumentLoader.loadDocument(tempFile.getAbsolutePath());
            }

            document.metadata().put("name", originalFilename);
            document.metadata().put("type", docType != null ? docType : "");
            document.metadata().put("size", file.getSize() + "B");

            List<TextSegment> semanticSegments = semanticSplitter.split(document);

            List<Embedding> embeddings = embeddingModel.embedAll(semanticSegments).content();
            pineconeEmbeddingStore.addAll(embeddings, semanticSegments);

            Files.deleteIfExists(tempFile.toPath());

            log.info("文档[{}]处理完成，成功入库{}个语义块", originalFilename, semanticSegments.size());
            return String.format("文档[%s]上传并入库成功，生成%d个语义块", originalFilename, semanticSegments.size());
        } catch (Exception e) {
            log.error("文档处理失败", e);
            return "文档处理失败：" + e.getMessage();
        }
    }
}
