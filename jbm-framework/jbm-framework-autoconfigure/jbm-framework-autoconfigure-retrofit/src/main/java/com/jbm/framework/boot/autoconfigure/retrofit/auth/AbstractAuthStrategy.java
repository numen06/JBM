package com.jbm.framework.boot.autoconfigure.retrofit.auth;

import com.jbm.framework.boot.autoconfigure.retrofit.AbstractStrategy;
import okhttp3.Request;

/**
 * @author wesley
 */
public abstract class AbstractAuthStrategy extends AbstractStrategy implements AuthStrategy {


    public <T> T getService(final Class<T> service) {
        return retrofit.create(service);
    }
    /**
     * @param originalRequest
     * @param authRequestBuilder
     */
    @Override
    public void generateToken(Request originalRequest, Request.Builder authRequestBuilder) {

    }

    /**
     *
     */
    @Override
    public void clearToken() {

    }

    /**
     *
     */
    @Override
    public void refreshToken() {

    }
}
