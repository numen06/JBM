package com.jbm.cluster.common.exception;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.mvc.GlobalDefaultExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义未认证处理
 *
 * @author wesley.zhang
 */
@Slf4j
public class JbmAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException, ServletException {
        ResultBody resultBody = GlobalDefaultExceptionHandler.resolveException(exception,request.getRequestURI());
        response.setStatus(resultBody.getHttpStatus());
        writeJson(response, resultBody);
    }

    /**
     * 客户端返回JSON字符串
     *
     * @param response
     * @param object
     * @return
     */
    private static void writeJson(HttpServletResponse response, Object object) {
        writeJson(response, JSON.toJSONString(object), MediaType.APPLICATION_JSON_UTF8_VALUE);
    }

    private static void writeJson(HttpServletResponse response, String string, String type) {
        try {
            response.setContentType(type);
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(string);
            response.getWriter().flush();
            response.getWriter().close();
        } catch (IOException e) {
        }
    }
}