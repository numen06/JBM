package jbm.framework.boot.autoconfigure.filter;

import com.jbm.framework.metadata.bean.ResultBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @Created wesley.zhang
 * @Date 2022/5/21 19:28
 * @Description TODO
 */
public interface UnknownRuntimeExceptionFilter {

    void apply(ResultBody resultBody, RuntimeException runtimeException, HttpServletRequest request);


}
