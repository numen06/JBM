package com.jbm.cluster.weixin.miniapp.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.enums.TicketType;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @program: JBM
 * @description: 微信分享签名
 * @author: Wu.Zhu
 * @create 2025/9/17 上午11:31
 **/

@Slf4j
@RestController
@RequestMapping("/mp/sign")
public class WxMpSignController {

    @Autowired
    private WxMpService wxService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/getSignature")
    public ResultBody getSignature(@RequestParam(value = "url") String url) throws WxErrorException {
        String jsapiTicket = stringRedisTemplate.opsForValue().get("jsapiTicket");
        if (StringUtils.isBlank(jsapiTicket)) {
            try {
                jsapiTicket = wxService.getJsapiTicket();
                stringRedisTemplate.opsForValue().set("jsapiTicket", jsapiTicket, 7200L, TimeUnit.SECONDS);
            } catch (WxErrorException e) {
                log.error("获取微信jsapi_ticket失败", e);
                throw new WxErrorException(e);
            }
        }
        String random = RandomUtil.randomString(16);
        long timestamp = new Date().getTime();
        String signature = "jsapi_ticket=" + jsapiTicket + "&noncestr=" + random + "&timestamp=" + timestamp + "&url=" + url;
        String sha1 = SecureUtil.sha1(signature);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("noncestr", random);
        resultMap.put("timestamp", timestamp);
        resultMap.put("signature", sha1);
        return ResultBody.ok().data(resultMap);
    }
}
