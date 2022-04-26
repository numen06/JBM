package com.jbm.cluster.api.entitys.message;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformTreeEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-04 21:21
 **/
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("push_item")
@ApiModel("配送设置管理")
public class PushItem extends MultiPlatformTreeEntity {

    @ApiModelProperty("推送项名称")
    private String name;

    @ApiModelProperty("推送项描述")
    private String description;

    @ApiModelProperty("是否启用email")
    private Boolean enableEmail;

    @ApiModelProperty("是否启用短信")
    private Boolean enableSms;

    @ApiModelProperty("是否启用推送")
    private Boolean enablePush;

    @ApiModelProperty("应用ID")
    private Long applicationId;

}
