package com.jbm.cluster.api.entitys.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务日志实体类
 * 用于存储业务模块的日志信息
 * 
 * @author wesley
 */
@Data
@ApiModel(value = "业务日志实体")
public class BusinessLog implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 业务日志ID，用于追踪和追加日志
     */
    @ApiModelProperty(value = "业务日志ID")
    private String logId;
    
    /**
     * 业务模块名称
     */
    @ApiModelProperty(value = "业务模块名称")
    private String module;
    
    /**
     * 业务操作类型
     */
    @ApiModelProperty(value = "业务操作类型")
    private String operation;
    
    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户ID")
    private String userId;
    
    /**
     * 用户名
     */
    @ApiModelProperty(value = "用户名")
    private String username;
    
    /**
     * 日志内容（单行或多行）
     */
    @ApiModelProperty(value = "日志内容")
    private String content;
    
    /**
     * 行号（用于文件型日志，标识这是第几行）
     */
    @ApiModelProperty(value = "行号")
    private Integer lineNumber;
    
    /**
     * 是否为追加内容（true: 追加的新内容, false: 创建时的初始内容）
     */
    @ApiModelProperty(value = "是否为追加内容")
    private Boolean isAppend;
    
    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    
    /**
     * 最后更新时间
     */
    @ApiModelProperty(value = "最后更新时间")
    private Date updateTime;
    
    /**
     * 过期时间（天数），默认30天
     */
    @ApiModelProperty(value = "过期时间（天数）")
    private Integer expireDays;
    
    /**
     * 实际过期日期
     */
    @ApiModelProperty(value = "实际过期日期")
    private Date expireDate;
    
    /**
     * 日志状态：ACTIVE-活跃, ARCHIVED-已归档, EXPIRED-已过期
     */
    @ApiModelProperty(value = "日志状态")
    private String status;
    
    /**
     * 请求IP
     */
    @ApiModelProperty(value = "请求IP")
    private String requestIp;
    
    /**
     * 追踪ID（用于关联多个相关日志）
     */
    @ApiModelProperty(value = "追踪ID")
    private String traceId;
    
    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;
    
    // ==================== 集成模块字段 ====================
    
    /**
     * 业务类型（如：订单、支付、用户等）
     * 用于集成模块
     */
    @ApiModelProperty(value = "业务类型")
    private String businessType;
    
    /**
     * 业务ID（如：订单号、支付流水号等）
     * 用于集成模块
     */
    @ApiModelProperty(value = "业务ID")
    private String businessId;
    
    /**
     * 日志来源（如：订单服务、支付服务等）
     * 用于集成模块
     */
    @ApiModelProperty(value = "日志来源")
    private String source;

    // ==================== 阶段进度扩展 ====================

    /**
     * 阶段编码
     */
    @ApiModelProperty(value = "阶段编码")
    private String stageCode;

    /**
     * 阶段名称
     */
    @ApiModelProperty(value = "阶段名称")
    private String stageName;

    /**
     * 阶段序号
     */
    @ApiModelProperty(value = "阶段序号")
    private Integer stageIndex;

    /**
     * 阶段进度
     */
    @ApiModelProperty(value = "阶段进度（0-100）")
    private Integer stageProgress;

    /**
     * 阶段状态
     */
    @ApiModelProperty(value = "阶段状态：WAITING、RUNNING、DONE、FAILED")
    private String stageStatus;

    /**
     * 阶段总数
     */
    @ApiModelProperty(value = "阶段总数")
    private Integer stageCount;

    /**
     * 整体进度
     */
    @ApiModelProperty(value = "整体进度（0-100）")
    private Integer overallProgress;

    /**
     * 是否阶段事件
     */
    @ApiModelProperty(value = "是否阶段事件")
    private Boolean stageEvent;
}

