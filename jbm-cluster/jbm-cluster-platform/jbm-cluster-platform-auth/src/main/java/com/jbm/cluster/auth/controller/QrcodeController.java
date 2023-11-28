package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.google.common.collect.Maps;
import com.jbm.cluster.auth.model.AuthConfirmModel;
import com.jbm.cluster.auth.service.ConfirmService;
import com.jbm.cluster.common.basic.annotation.AccessLogIgnore;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "二维码中心")
@RestController
@RequestMapping("/qrcode")
public class QrcodeController {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ConfirmService confirmService;


    @ApiOperation(value = "登录二维码")
    @GetMapping(value = "/login")
    public ResultBody login(@RequestParam String redirect_uri, @RequestParam String client_id, @RequestParam Integer width, @RequestParam Integer height) {
        Map<String, Object> result = Maps.newHashMap();
        AuthConfirmModel authConfirmModel = confirmService.create(client_id);
        redirect_uri = SaOAuth2Util.buildRedirectUri(redirect_uri, authConfirmModel.getCode(), authConfirmModel.getState());
        String image = QrCodeUtil.generateAsBase64(redirect_uri, QrConfig.create().setWidth(width).setHeight(height), ImgUtil.IMAGE_TYPE_PNG);
        result.put("image", image);
        result.put("code", authConfirmModel.getCode());
        result.put("state", authConfirmModel.getState());
        return ResultBody.ok(result);
    }


    @AccessLogIgnore
    @ApiOperation(value = "验证二维码登录")
    @GetMapping(value = "/check")
    public ResultBody check(@RequestParam String code) {
        try {
            AuthConfirmModel authConfirmModel = confirmService.checkConfirm(code);
            switch (authConfirmModel.getComfirmState()) {
                case 2:
                    return ResultBody.ok(SaOAuth2Util.getAccessToken(authConfirmModel.getToken()).toLineMap());
                default:
                    return ResultBody.failed().data(authConfirmModel.getComfirmState()).msg("请扫描");
            }

        } catch (SaTokenException saTokenException) {
            return ResultBody.failed().msg(saTokenException.getMessage());
        }
    }

    @ApiOperation(value = "已扫描")
    @GetMapping(value = "/scanned")
    public ResultBody scanned(@RequestParam String code) {
        try {
            AuthConfirmModel authConfirmModel = confirmService.changeConfirmState(code, 1);
            return ResultBody.ok().data(authConfirmModel.getComfirmState());
        } catch (SaTokenException saTokenException) {
            return ResultBody.failed().msg(saTokenException.getMessage());
        }
    }


}
