package com.jbm.cluster.auth.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class VCoderService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public String getVcodePath(String scope, String vcode) {
        String key = StrUtil.format("/vcode/{}/{}", StrUtil.blankToDefault(scope, "system"), vcode);
        return key;
    }

    public LineCaptcha build(String scope, Integer width, Integer height, Integer codeCount) {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(ObjectUtil.defaultIfNull(width, 200), ObjectUtil.defaultIfNull(height, 100), ObjectUtil.defaultIfNull(codeCount, 5), 100);
//        request.getSession().setAttribute("vcode", lineCaptcha.getCode());
        String key = this.getVcodePath(scope, lineCaptcha.getCode());
        stringRedisTemplate.opsForValue().set(key, lineCaptcha.getCode(), 1, TimeUnit.MINUTES);
        return lineCaptcha;
    }

    public Boolean verify(String vcode, String scope) {
        String key = this.getVcodePath(scope, vcode);
        boolean has = stringRedisTemplate.hasKey(key);
//        if (has) {
//            try {
//                stringRedisTemplate.delete(key);
//            } catch (Exception e) {
//
//            }
//        }
        return has;
    }
}
