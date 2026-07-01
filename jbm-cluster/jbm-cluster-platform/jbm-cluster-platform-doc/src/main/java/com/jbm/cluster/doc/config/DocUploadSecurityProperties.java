package com.jbm.cluster.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档服务上传扩展名白名单配置
 * <p>
 * 覆盖模式：{@code image-allowed-extensions} / {@code document-allowed-extensions} 非空时完全替换内置默认白名单。<br>
 * 追加模式：未配置覆盖列表时，在默认白名单基础上合并 {@code additional-*-extensions}。
 */
@Data
@ConfigurationProperties(prefix = "jbm.doc.upload")
public class DocUploadSecurityProperties {

    /**
     * 图片白名单（覆盖默认）
     */
    private List<String> imageAllowedExtensions = new ArrayList<>();

    /**
     * 文档白名单（覆盖默认）
     */
    private List<String> documentAllowedExtensions = new ArrayList<>();

    /**
     * 在默认图片白名单上追加的扩展名，如无需全量配置时使用
     */
    private List<String> additionalImageExtensions = new ArrayList<>();

    /**
     * 在默认文档白名单上追加的扩展名，例如 log、zip、json
     */
    private List<String> additionalDocumentExtensions = new ArrayList<>();
}
