package com.jbm.cluster.weixin.miniapp.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.binarywang.wx.miniapp.bean.WxMaUserInfo;
import com.jbm.cluster.weixin.miniapp.form.WxUserInfo;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.weixin.config.WxMaConfiguration;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 微信小程序用户接口
 */
@Slf4j
@RestController
@RequestMapping("/user/{appid}")
public class WxMaUserController {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 登陆接口
     */
    @GetMapping("/login")
    public ResultBody<WxMaJscode2SessionResult> login(@PathVariable String appid, String code) {
        return this.login2(appid, code);
    }

    @PostMapping("/login")
    public ResultBody<WxMaJscode2SessionResult> login2(@PathVariable String appid, String code) {
        if (StringUtils.isBlank(code)) {
            return ResultBody.error(null, "empty jscode");
        }
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);
        try {
            WxMaJscode2SessionResult session = wxService.getUserService().getSessionInfo(code);
            log.info(session.getSessionKey());
            log.info(session.getOpenid());
            //TODO 可以增加自己的逻辑，关联业务相关数据
            stringRedisTemplate.opsForValue().set(session.getSessionKey()+"-OPENID", session.getOpenid(), 5, TimeUnit.MINUTES);
            return ResultBody.success(session, "微信登录成功");
        } catch (WxErrorException e) {
            log.error(e.getMessage(), e);
            return ResultBody.error(null, e.getMessage());
        }
    }

    /**
     * <pre>
     * 获取用户信息接口
     * </pre>
     */
    @GetMapping("/info")
    public ResultBody<WxMaUserInfo> info(@PathVariable String appid, WxUserInfo wxUserInfo) {
        return this.info(appid, wxUserInfo);
    }

    @PostMapping("/info")
    public ResultBody<WxMaUserInfo> info2(@PathVariable String appid, @RequestBody WxUserInfo wxUserInfo) {
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);
        // 用户信息校验
        if (!wxService.getUserService().checkUserInfo(wxUserInfo.getSessionKey(), wxUserInfo.getRawData(), wxUserInfo.getSignature())) {
            return ResultBody.error(null, "user check failed");
        }
        // 解密用户信息
        WxMaUserInfo userInfo = wxService.getUserService().getUserInfo(wxUserInfo.getSessionKey(), wxUserInfo
                .getEncryptedData(), wxUserInfo.getIv());
        return ResultBody.success(userInfo, "微信获取用户成功");
    }


    /**
     * <pre>
     * 获取用户绑定手机号信息
     * </pre>
     */
    @GetMapping("/phone")
    public ResultBody<WxMaPhoneNumberInfo> phone(@PathVariable String appid, WxUserInfo wxUserInfo) {
        return this.phone2(appid, wxUserInfo);
    }

    /**
     * <pre>
     * 获取用户绑定手机号信息
     * </pre>
     */
    @PostMapping("/phone")
    public ResultBody<WxMaPhoneNumberInfo> phone2(@PathVariable String appid, @RequestBody WxUserInfo wxUserInfo) {
        final WxMaService wxService = WxMaConfiguration.getMaService(appid);
        if (!wxService.getUserService().checkUserInfo(wxUserInfo.getSessionKey(), wxUserInfo.getRawData(), wxUserInfo.getSignature())) {
            return ResultBody.error(null, "user check failed");
        }
        // 解密
        WxMaPhoneNumberInfo phoneNoInfo = wxService.getUserService().getPhoneNoInfo(wxUserInfo.getSessionKey(), wxUserInfo
                .getEncryptedData(), wxUserInfo.getIv());
        //将手机号临时放到缓存中5分钟
        stringRedisTemplate.opsForValue().set(wxUserInfo.getSessionKey()+"-PHONE", phoneNoInfo.getPhoneNumber(), 5, TimeUnit.MINUTES);
        return ResultBody.success(phoneNoInfo, "微信获取手机号成功");
    }

}
