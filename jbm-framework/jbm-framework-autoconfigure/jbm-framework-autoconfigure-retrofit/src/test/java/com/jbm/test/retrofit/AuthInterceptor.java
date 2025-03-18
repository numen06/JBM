package com.jbm.test.retrofit;

import com.jbm.framework.boot.autoconfigure.retrofit.ApiPlatform;
import com.jbm.framework.boot.autoconfigure.retrofit.interceptor.AbstractInterceptor;
import org.springframework.stereotype.Service;

/**
 * @author wesley
 */
@Service
@ApiPlatform(name = "platformB", strategys = {TestAuthStrategy.class})
public class AuthInterceptor extends AbstractInterceptor {


}
