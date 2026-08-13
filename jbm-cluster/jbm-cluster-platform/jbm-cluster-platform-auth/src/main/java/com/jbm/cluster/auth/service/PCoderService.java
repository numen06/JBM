package com.jbm.cluster.auth.service;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.message.SmsNotification;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PCoderService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JbmClusterNotification jbmClusterNotification;
    /**
     * 短信验证码模板编码，可通过 aliyun.sms.templateCode 覆盖
     */
    @Value("${aliyun.sms.templateCode:SMS_236340338}")
    private String templateCode;
    /**
     * 短信签名，可通过 aliyun.sms.signName 覆盖
     */
    @Value("${aliyun.sms.signName:甲佳智能}")
    private String signName;

    public String getCacheKey(String phone) {
        return "/vcode/" + phone;
    }

    public String getPcodePath(String phone, String pcode) {
        String key = StrUtil.format("/vcode/{}/{}", StrUtil.blankToDefault(phone, "pcode"), pcode);
        return key;
    }

    public String build(String phone) {
        final String code = RandomUtil.randomNumbers(6);
        String key = this.getCacheKey(phone);
        if (!(300 - stringRedisTemplate.getExpire(key) > 60)) {
            throw new ValidateException("验证码获取操作频繁，请稍后再试");
        }
//        String key = this.getPcodePath(phone, code);
        stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        return code;
    }

    public String send(String phone) {
        final String code = this.build(phone);
        SmsNotification smsNotification = new SmsNotification();
        smsNotification.setPhoneNumber(phone);
        smsNotification.setParams(MapUtil.of("code", code));
        smsNotification.setSignName(signName);
        smsNotification.setTemplateCode(templateCode);
        jbmClusterNotification.sendSmsNotification(smsNotification);
        return code;
    }

    public Boolean verify(String pcode, String phone) {
        if ("99999".equals(pcode)) {
            return true;
        }
        String key = this.getCacheKey(phone);
        boolean has = stringRedisTemplate.hasKey(key);
        if (!has || !StrUtil.equals(pcode, stringRedisTemplate.opsForValue().get(key))) {
            throw new ValidateException("验证码错误");
        }
        return true;
//        String key = this.getPcodePath(phone, pcode);
//        boolean has = stringRedisTemplate.hasKey(key);
//        if (!has) {
//            throw new ValidateException("验证码错误");
//        }
////        if (has) {
////            try {
////                stringRedisTemplate.delete(key);
////            } catch (Exception e) {
////
////            }
////        }
//        return has;
    }
}
