package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("open_api_operation")
@ApiModel("OpenAPI 接口索引")
public class OpenApiOperation extends MasterDataEntity {

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("操作 ID")
    private Long operationId;

    @ApiModelProperty("文档 ID")
    private Long docId;

    @ApiModelProperty("关联 base_api.api_id")
    private Long apiId;

    @ApiModelProperty("服务 ID")
    private String serviceId;

    @ApiModelProperty("路径")
    private String path;

    @ApiModelProperty("HTTP 方法")
    private String requestMethod;

    @ApiModelProperty("标签 JSON")
    private String tags;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("业务唯一键 serviceId:METHOD:path")
    private String operationKey;

    @ApiModelProperty("参数 JSON")
    private String parametersJson;

    @ApiModelProperty("请求体 JSON")
    private String requestBodyJson;

    @ApiModelProperty("响应 JSON")
    private String responsesJson;

    @ApiModelProperty("Schema JSON")
    private String schemasJson;

    @ApiModelProperty("安全要求 JSON")
    private String securityJson;

    @ApiModelProperty("示例 JSON")
    private String examplesJson;

    @ApiModelProperty("原始 operation JSON")
    private String rawOperationJson;

    @ApiModelProperty("是否废弃")
    private Integer deprecated;

    @ApiModelProperty("是否开放")
    private Integer isOpen;

    @ApiModelProperty("是否需要认证")
    private Integer isAuth;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("同步状态 NEW/ACTIVE/CHANGED/MISSING/FAILED")
    private String syncState;

    @ApiModelProperty("首次发现时间")
    private Date firstSeenTime;

    @ApiModelProperty("最近发现时间")
    private Date lastSeenTime;

    @ApiModelProperty("消失时间")
    private Date removedTime;

    @ApiModelProperty("变更类型")
    private String changeType;

    @ApiModelProperty("内容哈希")
    private String sourceHash;

    @ApiModelProperty("同步时间")
    private Date syncTime;
}
