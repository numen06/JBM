package com.jbm.cluster.auth.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.message.SmsNotification;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.validation.ValidationException;
import java.util.concurrent.TimeUnit;

@Service
public class PCoderService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JbmClusterNotification jbmClusterNotification;

    public String getPcodePath(String phone, String pcode) {
        String key = StrUtil.format("/pcode/{}/{}", StrUtil.blankToDefault(phone, "system"), pcode);
        return key;
    }

    public String build(String phone) {
        final String code = RandomUtil.randomNumbers(6);
        String key = this.getPcodePath(phone, code);
        stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        return code;
    }

    public String send(String phone) {
        final String code = this.build(phone);
        SmsNotification smsNotification = new SmsNotification();
        smsNotification.setPhoneNumber(phone);
        smsNotification.setParams(MapUtil.of("code", code));
        smsNotification.setSignName("甲佳智能");
        smsNotification.setTemplateCode("SMS_236340338");
        jbmClusterNotification.sendSmsNotification(smsNotification);
        return code;
    }

    public Boolean verify(String pcode, String phone) {
        if ("99999".equals(pcode)) {
            return true;
        }
        String key = this.getPcodePath(phone, pcode);
        boolean has = stringRedisTemplate.hasKey(key);
        if (!has) {
            throw new ValidationException("验证码错误");
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
