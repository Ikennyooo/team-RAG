package com.ikkkk.teamrag.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LongTermMemoryResponse {

    /** 服务端为本条录入生成的记录 ID */
    private String memoryRecordId;

    /** 写入向量库的文本块数量 */
    private int chunkCount;
}
