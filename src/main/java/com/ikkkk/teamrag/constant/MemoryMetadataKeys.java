package com.ikkkk.teamrag.constant;

/**
 * 向量片段元数据键与长期记忆来源标识（与 Pinecone 中存储的 metadata 一致）。
 */
public final class MemoryMetadataKeys {

    /** 来源字段名 */
    public static final String SOURCE = "source";

    /** 用户主动录入的长期记忆 */
    public static final String SOURCE_LONG_TERM_MEMORY = "LONG_TERM_MEMORY";

    private MemoryMetadataKeys() {
    }
}
