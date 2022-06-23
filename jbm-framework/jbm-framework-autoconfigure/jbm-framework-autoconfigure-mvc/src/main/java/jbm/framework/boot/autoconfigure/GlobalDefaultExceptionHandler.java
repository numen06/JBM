package jbm.framework.boot.autoconfigure;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.exceptions.DemoModeException;
import com.jbm.framework.exceptions.InnerAuthException;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.exceptions.auth.NotPermissionException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.WebExceptionResolve;
import jbm.framework.boot.autoconfigure.filter.UnknownRuntimeExceptionFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 全局异常
 * 统一异常处理器
 *
 * @author wesley.zhang
 * @date 2017/7/3
 */
@ControllerAdvice
@RestControllerAdvice
@ResponseBody
@Slf4j
public class GlobalDefaultExceptionHandler {

    @Autowired
    private ApplicationContext applicationContext;


    /**
     * 统一返回结果集
     *
     * @param resultBody
     * @param response
     * @return
     */
    public ResultBody returnResult(ResultBody resultBody, HttpServletResponse response) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        if (ObjectUtil.isNotEmpty(resultBody.getHttpStatus())) {
            response.setStatus(resultBody.getHttpStatus());
        }
        if (StrUtil.isBlank(resultBody.getMessage())) {
            resultBody.setMessage(WebExceptionResolve.DEF_ERROR_MSG);
        }
        return resultBody;
    }


    /**
     * 统一异常处理
     * AuthenticationException
     *
     * @param ex
     * @param request
     * @param response
     * @return
     */
//    @ExceptionHandler({AuthenticationException.class})
    public ResultBody authenticationException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResultBody resultBody = WebExceptionResolve.resolveException(ex, request.getRequestURI());
        return returnResult(resultBody, response);
    }

    /**
     * 权限码异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResultBody handleNotPermissionException(NotPermissionException e, HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限码校验失败'{}'", requestURI, e.getMessage());
        ResultBody resultBody = ResultBody.failed().httpStatus(HttpStatus.FORBIDDEN.value()).msg("没有访问权限，请联系管理员授权");
        return returnResult(resultBody, response);
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResultBody handleRuntimeException(RuntimeException runtimeException, HttpServletRequest httpServletRequest, HttpServletResponse response) {
        String requestURI = httpServletRequest.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, runtimeException);
        //以上标准内容注入完成之后，搜索自定义配置
        ResultBody resultBody = WebExceptionResolve.resolveException(runtimeException, httpServletRequest.getRequestURI());
        Map<String, UnknownRuntimeExceptionFilter> unknownRuntimeExceptionFilterMap = applicationContext.getBeansOfType(UnknownRuntimeExceptionFilter.class);
        unknownRuntimeExceptionFilterMap.forEach(new BiConsumer<String, UnknownRuntimeExceptionFilter>() {
            @Override
            public void accept(String s, UnknownRuntimeExceptionFilter preRequestInterceptor) {
                try {
                    preRequestInterceptor.apply(resultBody, runtimeException, httpServletRequest);
                } catch (Exception e) {
                    log.error("异常解析器[{}]失败", s, e);
                }
            }
        });
        return returnResult(resultBody, response);
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
        if (ObjectUtil.isNotEmpty(ex.getMessage())) {
            resultBody.msg(ex.getMessage());
        }
        return returnResult(resultBody, response);
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResultBody handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        ResultBody resultBody = ResultBody.error(e);
        return returnResult(resultBody, response);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public ResultBody handleBindException(BindException e, HttpServletResponse response) {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        ResultBody resultBody = ResultBody.failed().msg(message);
        return returnResult(resultBody, response);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultBody handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletResponse response) {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        ResultBody resultBody = ResultBody.failed().msg(message);
        return returnResult(resultBody, response);
    }

    /**
     * 内部认证异常
     */
    @ExceptionHandler(InnerAuthException.class)
    public ResultBody handleInnerAuthException(InnerAuthException e, HttpServletResponse response) {
        ResultBody resultBody = ResultBody.failed().msg(e.getMessage());
        return returnResult(resultBody, response);
    }

    /**
     * 演示模式异常
     */
    @ExceptionHandler(DemoModeException.class)
    public ResultBody handleDemoModeException(DemoModeException e, HttpServletResponse response) {
        ResultBody resultBody = ResultBody.failed().msg("演示模式，不允许操作");
        return returnResult(resultBody, response);
    }


}

