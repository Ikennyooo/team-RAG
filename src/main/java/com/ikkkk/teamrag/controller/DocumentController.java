package com.ikkkk.teamrag.controller;

import com.ikkkk.teamrag.bean.DocumentForm;
import com.ikkkk.teamrag.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文档管理", description = "团队 RAG 知识库文档上传接口")
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "文档上传并入库向量库")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(
            @Parameter(description = "上传的文件", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "文档类型（可选）") @RequestPart(value = "docType", required = false) String docType,
            @Parameter(description = "文档描述（可选）") @RequestPart(value = "description", required = false) String description) {
        return documentService.uploadAndStore(file, docType);
    }
}
