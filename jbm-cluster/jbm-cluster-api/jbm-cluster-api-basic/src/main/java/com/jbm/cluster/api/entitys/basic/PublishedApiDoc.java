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
@TableName("published_api_doc")
@ApiModel("已发布公开 API 文档")
public class PublishedApiDoc extends MasterDataEntity {

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("发布 ID")
    private Long publishedId;

    @ApiModelProperty("文档键")
    private String docKey;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("版本")
    private String version;

    @ApiModelProperty("内容类型")
    private String contentType;

    @ApiModelProperty("已发布 spec")
    private String publishedSpec;

    @ApiModelProperty("发布摘要")
    private String publishedSummary;

    @ApiModelProperty("内容哈希")
    private String sourceHash;

    @ApiModelProperty("发布人")
    private Long publisherUserId;

    @ApiModelProperty("发布时间")
    private Date publishedAt;

    @ApiModelProperty("状态 1=有效 0=禁用")
    private Integer status;
}
