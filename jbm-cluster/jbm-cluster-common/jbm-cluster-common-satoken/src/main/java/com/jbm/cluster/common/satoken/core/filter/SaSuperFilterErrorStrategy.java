package com.jbm.cluster.common.satoken.core.filter;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.filter.SaFilterErrorStrategy;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

/**
 * @Created wesley.zhang
 * @Date 2022/5/27 16:51
 * @Description TODO
 */
@Slf4j
public class SaSuperFilterErrorStrategy implements SaFilterErrorStrategy {
    @Override
    public Object run(Throwable e) {
//        log.warn("权限拦截异常", e);
        String error = "服务认证失败，无法访问系统资源";
        if (e instanceof SaTokenException) {
            error = e.getMessage();
        }
        return ResultBody.failed().msg(error)
                .httpStatus(HttpStatus.UNAUTHORIZED.value()).code(HttpStatus.UNAUTHORIZED.value());
    }
}
