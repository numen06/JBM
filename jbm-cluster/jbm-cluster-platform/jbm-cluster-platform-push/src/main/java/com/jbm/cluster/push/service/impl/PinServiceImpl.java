package com.jbm.cluster.push.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.push.service.PinService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.util.StringUtils;
import jbm.framework.aliyun.sms.AliyunSmsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


/**
 * @author: create by wesley
 * @date:2019/4/10
 */
@Service
public class PinServiceImpl implements PinService {

    @Autowired(required = false)
    private AliyunSmsTemplate aliyunSmsTemplate;

    @Value("${jbm.push.jaja7-dry-run:false}")
    private boolean jaja7DryRun;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    public String buildCode(String phoneNumber) {
        final String key = JbmConstants.QR_PREFIX + phoneNumber;
        String code = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(code)) {
            return code;
        }
        code = RandomUtil.randomNumbers(4);
        stringRedisTemplate.opsForValue().set(key, code, 30, TimeUnit.MINUTES);
        return code;
    }

    /**
     * 发送手机验证码
     *
     * @param phoneNumber
     * @return
     * @throws Exception
     */
    @Override
//    @Cacheable(value = "pin#3#0", key = "#phoneNumber")
    public JSONObject sendPinCode(String phoneNumber) throws Exception {
        final String key = JbmConstants.PIN_PREFIX + phoneNumber;
        Integer count = NumberUtil.parseInt(stringRedisTemplate.opsForValue().get(key));
        if (count > 3) {
            throw new ServiceException("短信数量超限请稍后重试");
        }
        count++;
        final String code = buildCode(phoneNumber);
        JSONObject ret;
        if (jaja7DryRun || aliyunSmsTemplate == null) {
            ret = new JSONObject();
            ret.put("Code", "OK");
            ret.put("Message", "jaja7 dry-run: SMS not sent");
        } else {
            ret = aliyunSmsTemplate.sendPin(code, phoneNumber);
            if (!"OK".equalsIgnoreCase(ret.getString("Code"))) {
                throw new ServiceException("发送失败");
            }
        }
        ret.put("pin", code);
        stringRedisTemplate.opsForValue().set(key, count.toString(), 30, TimeUnit.MINUTES);
        return ret;
    }

    @Override
    public Boolean vifCode(String phoneNumber, String code) {
        String oldCode = buildCode(phoneNumber);
        return StringUtils.equals(code, oldCode);
    }


}
