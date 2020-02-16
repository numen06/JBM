package jbm.framework.boot.autoconfigure;

import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.WebExceptionResolve;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 统一异常处理器
 *
 * @author wesley.zhang
 * @date 2017/7/3
 */
@ControllerAdvice
@ResponseBody
@Slf4j
public class GlobalDefaultExceptionHandler {


    /**
     * 统一异常处理
     * AuthenticationException
     *
     * @param ex
     * @param request
     * @param response
     * @return
     */
    @ExceptionHandler({AuthenticationException.class})
    public ResultBody authenticationException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResultBody resultBody = WebExceptionResolve.resolveException(ex, request.getRequestURI());
        response.setStatus(resultBody.getHttpStatus());
        return resultBody;
    }


    /**
     * 自定义异常
     *
     * @param ex
     * @param request
     * @param response
     * @return
     */
    @ExceptionHandler({ServiceException.class})
    public ResultBody openException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResultBody resultBody = WebExceptionResolve.resolveException(ex, request.getRequestURI());
        response.setStatus(resultBody.getHttpStatus());
        return resultBody;
    }

    /**
     * 其他异常
     *
     * @param ex
     * @param request
     * @param response
     * @return
     */
    @ExceptionHandler({Exception.class})
    public ResultBody exception(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResultBody resultBody = WebExceptionResolve.resolveException(ex, request.getRequestURI());
        response.setStatus(resultBody.getHttpStatus());
        return resultBody;
    }

}

