package com.jbm.cluster.common.feign;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.framework.metadata.bean.ResultBody;
import feign.FeignException;
import jbm.framework.web.exception.UnknownRuntimeExceptionFilter;

import javax.servlet.http.HttpServletRequest;

public class FeignUnknownRuntimeExceptionFilter implements UnknownRuntimeExceptionFilter {
    @Override
    public void apply(ResultBody resultBody, RuntimeException runtimeException, HttpServletRequest request) {
        if (runtimeException instanceof FeignException) {
            FeignException feignException = (FeignException) runtimeException;
            ResultBody feginResultBody = JSON.parseObject(StrUtil.str(feignException.responseBody().get(), CharsetUtil.UTF_8), ResultBody.class);
            //默认将错误引入消息
            resultBody.msg(feginResultBody.getMessage());
            //擦除报错信息
            resultBody.exception(null);
        }
    }
}
