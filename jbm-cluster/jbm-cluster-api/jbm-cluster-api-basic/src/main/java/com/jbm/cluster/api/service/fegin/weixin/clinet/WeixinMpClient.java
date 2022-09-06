package com.jbm.cluster.api.service.fegin.weixin.clinet;

import com.jbm.cluster.api.entitys.message.WeixinNotification;
import com.jbm.cluster.api.service.fegin.weixin.IWxMpTemplateMsg;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 微信公众号发送程序
 */
@Component
@FeignClient(value = JbmClusterConstants.WEIXIN_SERVER, path = "/mp")
public interface WeixinMpClient extends IWxMpTemplateMsg {

    @ApiOperation(value = "发送模板消息")
    @PostMapping("/templateMsg/sendTemplateMsg")
    ResultBody<WeixinNotification> sendTemplateMsg(@RequestBody WeixinNotification weixinNotification);
}
