package com.jbm;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class test extends TestCase {

    public void testApiFilter(){
        GatewayLogInfo gatewayLogInfo  = new GatewayLogInfo();
        gatewayLogInfo.setPath( "/auth/qrcode/check")   ;
        String realPath =gatewayLogInfo.getPath();
        realPath = StrUtil.removePrefix(realPath, "/");
        realPath = "/" + StrUtil.subAfter(realPath, "/", false);
        try {
            BaseApi baseApi = new BaseApi();
            baseApi.setAccessLog(false);
            if (ObjectUtil.isEmpty(baseApi)) {
                return;
            }
            gatewayLogInfo.setOperationType(baseApi.getBusinessScope());
            if (BooleanUtil.isFalse(baseApi.getAccessLog())) {
                gatewayLogInfo.setLoglevel(0);
            }
            gatewayLogInfo.setApiId(baseApi.getApiId());
            gatewayLogInfo.setApiName(baseApi.getApiName());
            gatewayLogInfo.setApiPath(baseApi.getPath());
            gatewayLogInfo.setPath(realPath);
        } catch (Exception e) {
            log.error("获取API信息异常", e);
        }

    }
}
