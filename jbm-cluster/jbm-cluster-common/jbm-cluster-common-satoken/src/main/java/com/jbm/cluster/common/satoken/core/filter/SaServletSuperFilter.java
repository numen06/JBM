package com.jbm.cluster.common.satoken.core.filter;

import cn.dev33.satoken.exception.BackResultException;
import cn.dev33.satoken.exception.StopMatchException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.util.SaTokenConsts;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.satoken.core.dao.RedisSaTokenDao;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * 增强版Auth拦截器
 *
 * @Created wesley.zhang
 * @Date 2022/5/27 16:09
 * @Description TODO
 */
@Order(SaTokenConsts.ASSEMBLY_ORDER)
public class SaServletSuperFilter extends SaServletFilter {

    public SaServletSuperFilter() {
        //将标准的默认拦截器设置进去
        this.setError(new SaSuperFilterErrorStrategy());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 执行全局过滤器
            SaRouter.match(super.getIncludeList()).notMatch(super.getExcludeList()).check(r -> {
                beforeAuth.run(null);
                auth.run(null);
            });

        } catch (StopMatchException e) {

        } catch (Throwable e) {
            // 1. 获取异常处理策略结果
            Object result = (e instanceof BackResultException) ? e.getMessage() : error.run(e);
            if (ObjectUtil.isNotEmpty(result)) {
                if (result instanceof ResultBody) {
                    String error = JSON.toJSONString(result);
                    result = error;
                    // 2. 写入输出流
                    if (response.getContentType() == null) {
                        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
                    }
                }
            }
            // 2. 写入输出流
            if (response.getContentType() == null) {
                response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            }
            response.getWriter().print(result);
            return;
        }

        // 执行
        chain.doFilter(request, response);
    }
}
