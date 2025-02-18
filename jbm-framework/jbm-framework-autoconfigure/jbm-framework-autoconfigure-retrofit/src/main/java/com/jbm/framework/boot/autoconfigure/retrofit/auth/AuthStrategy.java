package com.jbm.framework.boot.autoconfigure.retrofit.auth;

import com.jbm.framework.boot.autoconfigure.retrofit.Strategy;
import okhttp3.Request;

/**
 * @author wesley
 */
public interface AuthStrategy extends Strategy {

    void generateToken(Request originalRequest, Request.Builder authRequestBuilder);

    void clearToken();

    void refreshToken();

}
