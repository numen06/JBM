package com.jbm.cluster.common.basic.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author fanscat
 * @createTime 2024/6/7 10:28
 */
public interface TenantService {

    String CREATE_SCHEMA = "CREATE SCHEMA %s COLLATE utf8_general_ci";

    String SELECT_SCHEMA = "SELECT * FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '%s'";

    /**
     * 多租户切换数据源
     *
     * @param request
     * @param response
     * @param handler
     * @throws Exception
     */
    void changeDataSource(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

    /**
     * 清除上下文
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    void clearContext(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception;
}
