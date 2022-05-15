package com.jbm.cluster.platform.gateway.service.impl;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.platform.gateway.config.properties.CaptchaProperties;
import com.jbm.cluster.platform.gateway.enums.CaptchaType;
import com.jbm.cluster.platform.gateway.service.ValidateCodeService;
import com.jbm.framework.exceptions.CaptchaException;
import com.jbm.framework.exceptions.user.CaptchaExpireException;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 验证码实现处理
 *
 * @author wesley.zhang
 */
@Service
public class ValidateCodeServiceImpl implements ValidateCodeService {
    @Autowired
    private CaptchaProperties captchaProperties;
    @Autowired
    private RedisService redisService;

    /**
     * 生成验证码
     */
    @Override
    public ResultBody<Map<String, Object>> createCaptcha() throws IOException, CaptchaException {
        Map<String, Object> ajax = new HashMap<>();
        boolean captchaOnOff = captchaProperties.getEnabled();
        ajax.put("captchaOnOff", captchaOnOff);
        if (!captchaOnOff) {
            return ResultBody.ok(ajax);
        }

        // 保存验证码信息
        String uuid = IdUtil.simpleUUID();
        String verifyKey = JbmConstants.CAPTCHA_CODE_KEY + uuid;
        // 生成验证码
        CaptchaType captchaType = captchaProperties.getType();
        boolean isMath = CaptchaType.MATH == captchaType;
        Integer length = isMath ? captchaProperties.getNumberLength() : captchaProperties.getCharLength();
        CodeGenerator codeGenerator = ReflectUtil.newInstance(captchaType.getClazz(), length);
        AbstractCaptcha captcha = SpringUtil.getBean(captchaProperties.getCategory().getClazz());
        captcha.setGenerator(codeGenerator);
        captcha.createCode();
        String code = isMath ? getCodeResult(captcha.getCode()) : captcha.getCode();
        redisService.setCacheObject(verifyKey, code, JbmConstants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        ajax.put("uuid", uuid);
        ajax.put("img", captcha.getImageBase64());
        return ResultBody.ok(ajax);
    }

    private String getCodeResult(String capStr) {
        int numberLength = captchaProperties.getNumberLength();
        int a = Convert.toInt(StrUtil.sub(capStr, 0, numberLength).trim());
        char operator = capStr.charAt(numberLength);
        int b = Convert.toInt(StrUtil.sub(capStr, numberLength + 1, numberLength + 1 + numberLength).trim());
        switch (operator) {
            case '*':
                return Convert.toStr(a * b);
            case '+':
                return Convert.toStr(a + b);
            case '-':
                return Convert.toStr(a - b);
            default:
                return StrUtil.EMPTY;
        }
    }

    /**
     * 校验验证码
     */
    @Override
    public void checkCaptcha(String code, String uuid) throws CaptchaException {
        if (StrUtil.isEmpty(code)) {
            throw new CaptchaException("user.jcaptcha.not.blank");
        }
        if (StrUtil.isEmpty(uuid)) {
            throw new CaptchaExpireException();
        }
        String verifyKey = JbmConstants.CAPTCHA_CODE_KEY + uuid;
        String captcha = redisService.getCacheObject(verifyKey);
        redisService.deleteObject(verifyKey);

        if (!code.equalsIgnoreCase(captcha)) {
            throw new CaptchaException();
        }
    }
}
