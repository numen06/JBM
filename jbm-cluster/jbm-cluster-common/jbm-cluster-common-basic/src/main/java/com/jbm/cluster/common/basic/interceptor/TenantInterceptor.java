package com.jbm.cluster.common.basic.interceptor;

import com.jbm.cluster.common.basic.service.TenantService;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author fanscat
 * @createTime 2024/6/5 18:00
 */
public class TenantInterceptor implements HandlerInterceptor {

    private Collection<TenantService> services;

    public TenantInterceptor(Collection<TenantService> services) {
        this.services = services;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        for (Iterator<TenantService> iterator = services.iterator(); iterator.hasNext(); ) {
            TenantService service = iterator.next();
            service.changeDataSource(request, response, handler);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        for (Iterator<TenantService> iterator = services.iterator(); iterator.hasNext(); ) {
            TenantService service = iterator.next();
            service.clearContext(request, response, handler, ex);
        }
    }
}
