package com.jbm.test.token;

import cn.dev33.satoken.session.SaSession;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import org.junit.jupiter.api.Test;

/**
 * @Created wesley.zhang
 * @Date 2022/5/13 1:44
 * @Description TODO
 */
public class TokenTest {

    @Test
    public void dise() {
        String data = "{\"createTime\":1652377134128,\"dataMap\":{},\"id\":\"Authorization:login:session:sys_user:1\",\"timeout\":86276,\"tokenSignList\":[{\"device\":\"pc\",\"value\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOiJzeXNfdXNlcjoxIiwicm5TdHIiOiJWTURZZFg3OWlhZzAyTTZTSG5Ca2drNm1ZdWF5NW1rWCJ9.Wi810wnDX-Dnb6cNqbAIdWqIBv11mqBeNPidSUrLRHU\"}]}";
        SaSession saSession = JSON.parseObject(data, new TypeReference<SaSession>() {
        }.getType(), Feature.SupportNonPublicField);
        System.out.println(saSession);
    }
}
