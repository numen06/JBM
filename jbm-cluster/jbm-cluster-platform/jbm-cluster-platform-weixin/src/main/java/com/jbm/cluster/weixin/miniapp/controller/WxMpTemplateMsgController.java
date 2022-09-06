package com.jbm.cluster.weixin.miniapp.controller;

import cn.hutool.core.bean.BeanUtil;
import com.jbm.cluster.api.entitys.message.WeixinNotification;
import com.jbm.cluster.api.service.fegin.weixin.IWxMpTemplateMsg;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/mp/templateMsg")
public class WxMpTemplateMsgController implements IWxMpTemplateMsg {
    @Autowired
    private WxMpService wxService;

    @ApiOperation(value = "发送模板消息")
    @PostMapping("/sendTemplateMsg")
    @Override
    public ResultBody<WeixinNotification> sendTemplateMsg(@RequestBody WeixinNotification weixinNotification) {
        return ResultBody.callback("发送模板消息成功", () -> {
            try {
                WxMpTemplateMessage wxMpTemplateMessage = new WxMpTemplateMessage();
                BeanUtil.copyProperties(weixinNotification, wxMpTemplateMessage);
                String msg = this.wxService.getTemplateMsgService().sendTemplateMsg(wxMpTemplateMessage);
                log.info("微信发送模板信息反馈:{}", msg);
                return weixinNotification;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
