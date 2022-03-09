package com.jbm.cluster.weixin.miniapp.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.binarywang.wx.miniapp.bean.WxMaUserInfo;
import com.jbm.cluster.weixin.miniapp.form.WxUserInfo;
import jbm.framework.weixin.config.WxMaConfiguration;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 微信小程序用户接口
 */
@Slf4j
@RestController
@RequestMapping("/user/{appid}")
public class WxMaUserController {

    /**
     * 登陆接口
     */
    @GetMapping("/login")
    public Object login(@PathVariable String appid, String code) {
        if (StringUtils.isBlank(code)) {
            return "empty jscode";
        }

        final WxMaService wxService = WxMaConfiguration.getMaService(appid);

        try {
            WxMaJscode2SessionResult session = wxService.getUserService().getSessionInfo(code);
            log.info(session.getSessionKey());
            log.info(session.getOpenid());
            //TODO 可以增加自己的逻辑，关联业务相关数据
            return session;
        } catch (WxErrorException e) {
            log.error(e.getMessage(), e);
            return e.toString();
        }
    }

    /**
     * <pre>
     * 获取用户信息接口
     * </pre>
     */
    @GetMapping("/info")
    public Object info(@PathVariable String appid, WxUserInfo wxUserInfo) {
        return this.info(appid, wxUserInfo);
    }

    @PostMapping("/info")
    public Object info2(@PathVariable String appid, @RequestBody WxUserInfo wxUserInfo) {
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);

        // 用户信息校验
        if (!wxService.getUserService().checkUserInfo(wxUserInfo.getSessionKey(), wxUserInfo.getRawData(), wxUserInfo.getSignature())) {
            return "user check failed";
        }

        // 解密用户信息
        WxMaUserInfo userInfo = wxService.getUserService().getUserInfo(wxUserInfo.getSessionKey(), wxUserInfo
                .getEncryptedData(), wxUserInfo.getIv());

        return userInfo;
    }


    /**
     * <pre>
     * 获取用户绑定手机号信息
     * </pre>
     */
    @GetMapping("/phone")
    public Object phone(@PathVariable String appid, WxUserInfo wxUserInfo) {
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);
        // 用户信息校验
        if (!wxService.getUserService().checkUserInfo(wxUserInfo.getSessionKey(), wxUserInfo.getRawData(), wxUserInfo.getSignature())) {
            return "user check failed";
        }
        // 解密
        WxMaPhoneNumberInfo phoneNoInfo = wxService.getUserService().getPhoneNoInfo(wxUserInfo.getSessionKey(), wxUserInfo
                .getEncryptedData(), wxUserInfo.getIv());
        return phoneNoInfo;
    }

    /**
     * <pre>
     * 获取用户绑定手机号信息
     * </pre>
     */
    @PostMapping("/phone")
    public Object phone2(@PathVariable String appid, @RequestBody WxUserInfo wxUserInfo) {
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);
        if (!wxService.getUserService().checkUserInfo(wxUserInfo.getSessionKey(), wxUserInfo.getRawData(), wxUserInfo.getSignature())) {
            return "user check failed";
        }
        // 解密
        WxMaPhoneNumberInfo phoneNoInfo = wxService.getUserService().getPhoneNoInfo(wxUserInfo.getSessionKey(), wxUserInfo
                .getEncryptedData(), wxUserInfo.getIv());
        return phoneNoInfo;
    }

}
