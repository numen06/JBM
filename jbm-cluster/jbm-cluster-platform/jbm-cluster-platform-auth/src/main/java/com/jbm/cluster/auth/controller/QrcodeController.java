package com.jbm.cluster.auth.controller;

import afu.org.checkerframework.checker.oigj.qual.O;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.google.common.collect.Maps;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.auth.service.BaseAppPreprocessing;
import com.jbm.cluster.auth.service.PCoderService;
import com.jbm.cluster.auth.service.VCoderService;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

@Api(tags = "二维码中心")
@RestController
@RequestMapping("/qrcode")
public class QrcodeController {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @ApiOperation(value = "登录二维码")
    @GetMapping(value = "/login")
    public ResultBody login(@RequestParam String redirect_uri, @RequestParam Integer width, @RequestParam Integer height) {
        Map<String, Object> result = Maps.newHashMap();
        String image = QrCodeUtil.generateAsBase64(redirect_uri, QrConfig.create().setWidth(width).setHeight(height), ImgUtil.IMAGE_TYPE_PNG);
        result.put("image", image);
        final String code = IdUtil.fastUUID();
        stringRedisTemplate.opsForValue().set("code", redirect_uri);
        result.put("code", IdUtil.fastUUID());
        return ResultBody.ok(image);
    }


    @ApiOperation(value = "验证二维码登录")
    @GetMapping(value = "/check")
    public ResultBody check(@RequestParam String code) {
        if (!stringRedisTemplate.hasKey(code)) {
            return ResultBody.failed();
        }
        return ResultBody.ok();
    }


}
