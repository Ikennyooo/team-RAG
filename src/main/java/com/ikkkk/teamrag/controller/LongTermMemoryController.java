package com.ikkkk.teamrag.controller;

import com.ikkkk.teamrag.bean.LongTermMemoryRequest;
import com.ikkkk.teamrag.bean.LongTermMemoryResponse;
import com.ikkkk.teamrag.service.LongTermMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "长期记忆", description = "用户主动录入长期记忆并写入向量库")
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class LongTermMemoryController {

    private final LongTermMemoryService longTermMemoryService;

    @Operation(summary = "录入长期记忆并入库向量库")
    @PostMapping(value = "/long-term", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ingest(@RequestBody LongTermMemoryRequest request) {
        try {
            LongTermMemoryResponse body = longTermMemoryService.ingest(request);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
