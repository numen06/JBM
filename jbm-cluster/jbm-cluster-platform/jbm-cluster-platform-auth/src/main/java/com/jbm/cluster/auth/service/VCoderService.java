package com.jbm.cluster.auth.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.service.SysDebugModeService;
import com.jbm.cluster.core.constant.JbmConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class VCoderService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SysDebugModeService sysDebugModeService;

    public String getVcodePath(String scope, String vcode) {
        String codeKey = vcode.toLowerCase();
        return StrUtil.format("/vcode/{}/{}", StrUtil.blankToDefault(scope, "system"), codeKey);
    }

    public LineCaptcha build(String scope, Integer width, Integer height, Integer codeCount) {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(ObjectUtil.defaultIfNull(width, 200), ObjectUtil.defaultIfNull(height, 100), ObjectUtil.defaultIfNull(codeCount, 5), 50);
//        request.getSession().setAttribute("vcode", lineCaptcha.getCode());
        String key = this.getVcodePath(scope, lineCaptcha.getCode());
        stringRedisTemplate.opsForValue().set(key, lineCaptcha.getCode(), 1, TimeUnit.MINUTES);
        return lineCaptcha;
    }

    public Boolean verify(String vcode) {
        return this.verify(vcode, "system");
    }

    public Boolean verify(String vcode, String scope) {
        if (JbmConstants.DEBUG_CAPTCHA_CODE.equals(vcode) && sysDebugModeService.isDebugModeEnabled()) {
            return true;
        }
        String key = this.getVcodePath(scope, vcode);
        boolean has = stringRedisTemplate.hasKey(key);
        if (!has) {
            throw new ValidateException("验证码错误");
        }
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
