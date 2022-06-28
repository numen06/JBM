package com.jbm.cluster.auth.service;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.jbm.cluster.auth.model.AuthConfirmModel;
import com.jbm.framework.mvc.ServletUtils;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ConfirmService {
    @Autowired
    private RedisService redisService;


    private final String CONFIRM_PIX = "confirm:oauth2:code:";

    /**
     * 确认登录
     *
     * @param code
     */
    public void doConfirm(String code) {
        StpUtil.checkLogin();
        AuthConfirmModel authConfirmModel = this.checkConfirm(code);
        authConfirmModel.setToken(StpUtil.getTokenValue());
        authConfirmModel.setComfirmState(2);
        this.changeConfirmState(authConfirmModel);
    }

    public AuthConfirmModel create(String clientId) {
        AuthConfirmModel authConfirmModel = new AuthConfirmModel();
        authConfirmModel.setComfirmState(0);
        authConfirmModel.setCode(this.generateCode());
        authConfirmModel.setState(this.generateState());
        authConfirmModel.setToken(StpUtil.getTokenValue());
        authConfirmModel.setClientId(clientId);
        //设置登录设备
        UserAgent userAgent = UserAgentUtil.parse(ServletUtils.getRequest().getHeader("User-Agent"));
        authConfirmModel.setDevice(userAgent.getOs().getName());
        final String checkCode = this.buildKey(authConfirmModel.getCode());
        redisService.setCacheObject(checkCode, authConfirmModel, 1L, TimeUnit.MINUTES);
        return authConfirmModel;
    }

    public AuthConfirmModel checkConfirm(String code) {
        final String checkCode = this.buildKey(code);
        if (!redisService.hasKey(checkCode)) {
            throw new SaTokenException("非法确认码");
        }
        AuthConfirmModel authConfirmModel = redisService.getCacheObject(checkCode);
        return authConfirmModel;
    }

    public AuthConfirmModel changeConfirmState(String code, Integer confirmState) {
        final String checkCode = this.buildKey(code);
        if (!redisService.hasKey(checkCode)) {
            throw new SaTokenException("非法确认码");
        }
        AuthConfirmModel authConfirmModel = redisService.getCacheObject(checkCode);
        authConfirmModel.setComfirmState(confirmState);
        return this.changeConfirmState(authConfirmModel);
    }

    public AuthConfirmModel changeConfirmState(AuthConfirmModel authConfirmModel) {
        final String checkCode = this.buildKey(authConfirmModel.getCode());
        redisService.setCacheObject(checkCode, authConfirmModel, 1L, TimeUnit.MINUTES);
        return authConfirmModel;
    }

    public String generateCode() {
        return IdUtil.fastUUID();
    }

    public String generateState() {
        return IdUtil.nanoId();
    }

    private String buildKey(String code) {
        final String checkCode = CONFIRM_PIX + code;
        return checkCode;
    }


}
