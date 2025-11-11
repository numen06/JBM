package com.jbm.cluster.api.model.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 业务日志响应对象
 * 用于Feign客户端调用返回
 * 
 * @author wesley
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessLogResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private String logId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 过期天数
     */
    private Integer expireDays;

    /**
     * 日志来源
     */
    private String source;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 过期时间
     */
    private Long expireTime;

    /**
     * 日志状态（ACTIVE、EXPIRED、DELETED）
     */
    private String status;

    /**
     * 总行数
     */
    private Integer totalLines;

    /**
     * 日志访问URL
     */
    private String logUrl;

    /**
     * 临时访问URL（带签名）
     */
    private String temporaryUrl;

    /**
     * URL过期时间戳
     */
    private Long urlExpireTime;
}

