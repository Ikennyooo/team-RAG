package com.ikkkk.teamrag.bean;

import lombok.Data;

import java.util.List;

@Data
public class LongTermMemoryRequest {

    /** 需要长期保存的正文（必填） */
    private String content;

    /** 标题（可选） */
    private String title;

    /** 标签（可选） */
    private List<String> tags;

    /** 客户端生成的记忆 ID，便于幂等或后续删除（可选） */
    private String clientMemoryId;
}
