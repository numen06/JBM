package com.jbm.cluster.api.model.basic;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户 MQTT 订阅地址
 */
@Data
@NoArgsConstructor
@ApiModel("当前用户 MQTT 订阅地址")
public class CurrentUserSubscribeAddress {

    @ApiModelProperty("当前用户 ID，供扫码上传等客户端拼装参数")
    private Long userId;

    @ApiModelProperty("用户消息通知 topic，格式 user/{userId}")
    private String userNotify;

    @ApiModelProperty("账号即将到期通知 topic，未配置 trade MQTT 前缀时为 null")
    private String accountExpiringNotify;

    @ApiModelProperty("账号已到期通知 topic，未配置 trade MQTT 前缀时为 null")
    private String accountExpiredNotify;
}
