package com.jbm.framework.masterdata.code.model;

import com.jbm.framework.masterdata.code.constants.CodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单次代码生成记录条目，用于写入 autocode.yml。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedRecord {

    /** 实体类全限定名 */
    private String entity;
    /** 代码类型 */
    private String codeType;
    /** 输出文件绝对路径 */
    private String path;
    /** 生成时间戳，可选 */
    private String timestamp;

    public static GeneratedRecord of(String entity, CodeType codeType, String path, String timestamp) {
        return new GeneratedRecord(entity, codeType != null ? codeType.name() : null, path, timestamp);
    }
}
