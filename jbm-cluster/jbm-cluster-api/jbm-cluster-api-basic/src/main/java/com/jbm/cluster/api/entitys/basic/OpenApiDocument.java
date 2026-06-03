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
@TableName("open_api_document")
@ApiModel("OpenAPI 文档")
public class OpenApiDocument extends MasterDataEntity {

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("文档 ID")
    private Long docId;

    @ApiModelProperty("服务 ID")
    private String serviceId;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("版本")
    private String version;

    @ApiModelProperty("来源 URL")
    private String sourceUrl;

    @ApiModelProperty("规范版本")
    private String specVersion;

    @ApiModelProperty("原始 spec JSON")
    private String rawSpec;

    @ApiModelProperty("内容哈希")
    private String sourceHash;

    @ApiModelProperty("同步状态 SUCCESS/FAILED")
    private String syncStatus;

    @ApiModelProperty("同步消息")
    private String syncMessage;

    @ApiModelProperty("最近同步时间")
    private Date syncTime;
}
