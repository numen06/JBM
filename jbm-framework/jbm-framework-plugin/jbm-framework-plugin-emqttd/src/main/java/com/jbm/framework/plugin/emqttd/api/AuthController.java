package com.jbm.framework.plugin.emqttd.api;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.framework.plugin.emqttd.dto.AclReq;
import com.jbm.framework.plugin.emqttd.dto.AuthReq;
import com.jbm.framework.plugin.emqttd.dto.SuperReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-11-20 19:16
 **/
@Slf4j
@RestController
@RequestMapping("/mqtt")
public class AuthController {

    private Cache<Object, AuthReq> authCache = CacheUtil.newLRUCache(100);

    @RequestMapping("/auth")
    public void auth(AuthReq authReq, HttpServletResponse response) {
        log.info("auth");
        log.info(JSON.toJSONString(authReq));
        if (authCache.containsKey(authReq.getClientid()))
            return;
        response.setStatus(401);
    }


    @RequestMapping("/acl")
    public void acl(AclReq aclReq, HttpServletResponse response) {
        log.info("acl");
        log.info(JSON.toJSONString(aclReq));
        if (authCache.containsKey(aclReq.getClientid()))
            return;
        response.setStatus(401);
    }

    @RequestMapping("/superuser")
    public void superuser(SuperReq superReq, HttpServletResponse response) {
        log.info("superuser");
        log.info(JSON.toJSONString(superReq));
        if (authCache.containsKey(superReq.getClientid()))
            return;
        response.setStatus(401);
    }

    @RequestMapping("/getAuth")
    public AuthReq getUser(AuthReq authReq) {
        log.info("getAuth");
        log.info(JSON.toJSONString(authReq));
        if (authCache.containsKey(authReq.getClientid())){
            return authCache.get(authReq.getClientid());
        }
        authReq.setUsername(IdUtil.fastSimpleUUID());
        authReq.setPassword(IdUtil.fastSimpleUUID());
        authCache.put(authReq.getClientid(), authReq);
        log.info(JSON.toJSONString(authReq));
        return authReq;
    }


}
