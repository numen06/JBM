package com.jbm.cluster.api.service.feign.weixin;

import com.jbm.cluster.api.entitys.message.WeixinNotification;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;



public interface IWxMpTemplateMsg {
    @ApiOperation(value = "发送模板消息")
    @GetMapping("/sendTemplateMsg")
    ResultBody<WeixinNotification> sendTemplateMsg(@RequestBody WeixinNotification weixinNotification);
}
