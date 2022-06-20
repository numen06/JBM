package com.jbm.cluster.doc.model;

import lombok.Data;

@Data
public class FileReqBody {
    // 重命名用
    private String name;

    // 历史信息
    private String id;
    private int offset = 0;
    private int count;
}
