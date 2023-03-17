package com.jbm.cluster.api.entitys.message;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-03-04 21:21
 **/
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("push_config_info")
@ApiModel("配送设置管理")
public class PushConfigInfo extends MultiPlatformIdEntity {

    @ApiModelProperty("是否启用")
    private Boolean enable;

    @ApiModelProperty("类型：email,sms,push")
    private Long type;

    @Column(columnDefinition = "TEXT")
    @ApiModelProperty(value = "配置内容")
    private String releaseContent;
}
