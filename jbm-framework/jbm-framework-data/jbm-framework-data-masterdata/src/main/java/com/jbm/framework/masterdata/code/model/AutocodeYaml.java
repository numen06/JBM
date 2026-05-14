package com.jbm.framework.masterdata.code.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * autocode.yml 映射实体，用于 YamlUtil 序列化/反序列化。
 */
@Data
public class AutocodeYaml {

    /** 生成时间 */
    private String generatedAt;
    /** 实体及其生成文件列表 */
    private List<AutocodeItem> items = new ArrayList<>();

    @Data
    public static class AutocodeItem {
        /** 实体类全限定名 */
        private String entity;
        /** 该实体生成的各类型文件 */
        private List<AutocodeGenerated> generated = new ArrayList<>();
    }

    @Data
    public static class AutocodeGenerated {
        private String codeType;
        private String path;
        private String timestamp;
    }
}
