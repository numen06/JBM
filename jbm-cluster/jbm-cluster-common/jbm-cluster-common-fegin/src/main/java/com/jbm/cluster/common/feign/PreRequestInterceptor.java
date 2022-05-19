package com.jbm.cluster.common.feign;

import feign.RequestTemplate;

import javax.servlet.http.HttpServletRequest;

/**
 * @Created wesley.zhang
 * @Date 2022/5/19 19:09
 * @Description TODO
 */
public interface PreRequestInterceptor {

    void apply(RequestTemplate requestTemplate, HttpServletRequest httpServletRequest);
}
