package com.jbm.cluster.platform.gateway.handler;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.metadata.enumerate.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

/**
 * @program: jbm
 * @author: wesley.zhang
 * @create: 2020-02-16 23:09
 **/
@Slf4j
public class WebExceptionResolve {

    private final MessageSource messageSource;

    public WebExceptionResolve(MessageSource messageSource) {
        this.messageSource = messageSource;
    }


    /**
     * 静态解析异常。可以直接调用
     *
     * @param ex
     * @return
     */
    public ResultBody resolveException(Throwable ex, String path) {
        ErrorCode code = null;
        int httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String message = ex.getMessage();
        String superClassName = ex.getClass().getSuperclass().getName();
        String className = ex.getClass().getName();
        if (className.contains("ResponseStatusException")) {
            httpStatus = ((ResponseStatusException) ex).getStatus().value();
        }
        if (className.contains("UsernameNotFoundException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.USERNAME_NOT_FOUND;
        } else if (className.contains("BadCredentialsException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.BAD_CREDENTIALS;
        } else if (className.contains("AccountExpiredException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.ACCOUNT_EXPIRED;
        } else if (className.contains("LockedException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.ACCOUNT_LOCKED;
        } else if (className.contains("DisabledException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.ACCOUNT_DISABLED;
        } else if (className.contains("CredentialsExpiredException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.CREDENTIALS_EXPIRED;
        } else if (className.contains("InvalidClientException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.INVALID_CLIENT;
        } else if (className.contains("UnauthorizedClientException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.UNAUTHORIZED_CLIENT;
        } else if (className.contains("OAuth2AuthenticationException") || className.contains("InsufficientAuthenticationException") || className.contains("AuthenticationCredentialsNotFoundException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.UNAUTHORIZED;
        } else if (className.contains("InvalidGrantException")) {
            code = ErrorCode.ALERT;
            if ("Bad credentials".contains(message)) {
                code = ErrorCode.BAD_CREDENTIALS;
            } else if ("User is disabled".contains(message)) {
                code = ErrorCode.ACCOUNT_DISABLED;
            } else if ("User account is locked".contains(message)) {
                code = ErrorCode.ACCOUNT_LOCKED;
            }
        } else if (className.contains("InvalidScopeException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.INVALID_SCOPE;
        } else if (className.contains("InvalidTokenException")) {
            httpStatus = HttpStatus.UNAUTHORIZED.value();
            code = ErrorCode.INVALID_TOKEN;
        } else if (className.contains("InvalidRequestException")) {
            httpStatus = HttpStatus.BAD_REQUEST.value();
            code = ErrorCode.INVALID_REQUEST;
        } else if (className.contains("RedirectMismatchException")) {
            code = ErrorCode.REDIRECT_URI_MISMATCH;
        } else if (className.contains("UnsupportedGrantTypeException")) {
            code = ErrorCode.UNSUPPORTED_GRANT_TYPE;
        } else if (className.contains("UnsupportedResponseTypeException")) {
            code = ErrorCode.UNSUPPORTED_RESPONSE_TYPE;
        } else if (className.contains("UserDeniedAuthorizationException")) {
            code = ErrorCode.ACCESS_DENIED;
        } else if (className.contains("AccessDeniedException")) {
            code = ErrorCode.ACCESS_DENIED;
            httpStatus = HttpStatus.FORBIDDEN.value();
            if (ErrorCode.ACCESS_DENIED_BLACK_LIMITED.getMessage().contains(message)) {
                code = ErrorCode.ACCESS_DENIED_BLACK_LIMITED;
            } else if (ErrorCode.ACCESS_DENIED_WHITE_LIMITED.getMessage().contains(message)) {
                code = ErrorCode.ACCESS_DENIED_WHITE_LIMITED;
            } else if (ErrorCode.ACCESS_DENIED_AUTHORITY_EXPIRED.getMessage().contains(message)) {
                code = ErrorCode.ACCESS_DENIED_AUTHORITY_EXPIRED;
            } else if (ErrorCode.ACCESS_DENIED_UPDATING.getMessage().contains(message)) {
                code = ErrorCode.ACCESS_DENIED_UPDATING;
            } else if (ErrorCode.ACCESS_DENIED_DISABLED.getMessage().contains(message)) {
                code = ErrorCode.ACCESS_DENIED_DISABLED;
            } else if (ErrorCode.ACCESS_DENIED_NOT_OPEN.getMessage().contains(message)) {
                code = ErrorCode.ACCESS_DENIED_NOT_OPEN;
            }
        } else if (className.contains("HttpMessageNotReadableException")
                || className.contains("TypeMismatchException")
                || className.contains("MissingServletRequestParameterException")) {
            httpStatus = HttpStatus.BAD_REQUEST.value();
            code = ErrorCode.BAD_REQUEST;
        } else if (className.contains("NoHandlerFoundException")) {
            httpStatus = HttpStatus.NOT_FOUND.value();
            code = ErrorCode.NOT_FOUND;
        } else if (className.contains("HttpRequestMethodNotSupportedException")) {
            httpStatus = HttpStatus.METHOD_NOT_ALLOWED.value();
            code = ErrorCode.METHOD_NOT_ALLOWED;
        } else if (className.contains("HttpMediaTypeNotAcceptableException")) {
            httpStatus = HttpStatus.BAD_REQUEST.value();
            code = ErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE;
        } else if (className.contains("MethodArgumentNotValidException")) {
            BindingResult bindingResult = ((MethodArgumentNotValidException) ex).getBindingResult();
            code = ErrorCode.ALERT;
            return ResultBody.failed().code(code.getCode()).msg(bindingResult.getFieldError().getDefaultMessage());
        } else if (className.contains("IllegalArgumentException")) {
            //参数错误
            code = ErrorCode.ALERT;
            httpStatus = HttpStatus.BAD_REQUEST.value();
        } else if (className.contains("OpenAlertException")) {
            code = ErrorCode.ALERT;
        } else if (className.contains("OpenSignatureException")) {
            httpStatus = HttpStatus.BAD_REQUEST.value();
            code = ErrorCode.SIGNATURE_DENIED;
        } else if (StrUtil.isEmpty(message)) {
            code = ErrorCode.ERROR;
        } else if (message.equalsIgnoreCase(ErrorCode.TOO_MANY_REQUESTS.name())) {
            code = ErrorCode.TOO_MANY_REQUESTS;
        }
        return buildBody(ex, code, path, httpStatus);
    }

    /**
     * 构建返回结果对象
     *
     * @param exception
     * @return
     */
    public ResultBody buildBody(Throwable exception, ErrorCode errorCode, String path, int httpStatus) {
        String errorMsg = null;
        if (errorCode == null) {
            HttpStatus status = HttpStatus.valueOf(httpStatus);
            if (ObjectUtil.isNotEmpty(status)) {
                errorMsg = messageSource.getMessage(status.name().toLowerCase(), ArrayUtil.newArray(0), LocaleContextHolder.getLocale());
            }
            errorCode = ErrorCode.ERROR;
        } else {
            errorMsg = messageSource.getMessage(errorCode.getMessage(), ArrayUtil.newArray(0), LocaleContextHolder.getLocale());
        }
        if (StrUtil.isBlank(errorMsg)) {
            errorMsg = "服务器发生错误,请联系管理员处理。";
        }
        ResultBody resultBody = ResultBody.failed().code(errorCode.getCode()).msg(errorMsg)
                .path(path).httpStatus(httpStatus).exception(exception);
        log.error("==> error:{}", resultBody, exception);
        return resultBody;
    }

}
