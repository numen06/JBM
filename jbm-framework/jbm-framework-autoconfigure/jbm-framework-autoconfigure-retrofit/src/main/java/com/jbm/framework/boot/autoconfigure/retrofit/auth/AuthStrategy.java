package com.jbm.framework.boot.autoconfigure.retrofit.auth;

import com.jbm.framework.boot.autoconfigure.retrofit.BaseStrategy;
import okhttp3.Request;

import java.util.function.Supplier;

/**
 * @author wesley
 */
public interface AuthStrategy extends BaseStrategy {

    void generateToken(Request originalRequest, Request.Builder authRequestBuilder);

    void clearToken();

    void refreshToken();

}
