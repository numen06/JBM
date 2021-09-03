package com.jbm.cluster.api.model.entity.message;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.cluster.api.model.message.Notification;
import com.jbm.framework.masterdata.usage.entity.MasterDataCodeEntity;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
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
@TableName("push_config_info")
@ApiModel("配送设置管理")
public class PushConfigInfo extends MultiPlatformEntity {

    @ApiModelProperty("是否启用")
    private Boolean enable;

    @ApiModelProperty("类型：email,sms,push")
    private Long type;

    @Column(columnDefinition = "TEXT")
    @ApiModelProperty(value = "配置内容")
    private String releaseContent;
}
