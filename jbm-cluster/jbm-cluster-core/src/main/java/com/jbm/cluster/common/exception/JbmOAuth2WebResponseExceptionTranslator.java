package com.jbm.cluster.common.exception;

import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.mvc.GlobalDefaultExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 自定义oauth2异常提示
 * @author wesley.zhang
 */
@Slf4j
public class JbmOAuth2WebResponseExceptionTranslator implements WebResponseExceptionTranslator {

    @Override
    public ResponseEntity translate(Exception e) throws Exception {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        ResultBody responseData = GlobalDefaultExceptionHandler.resolveException(e,request.getRequestURI());
        return ResponseEntity.status(responseData.getHttpStatus()).body(responseData);
    }
}
