package com.jbm.framework.boot.autoconfigure.retrofit;

import okhttp3.Request;

/**
 * @author wesley
 */
public interface BaseStrategy  {

    void setPlatform(PlatformsProperties.Platform platform);

    void doStrategy(Request originalRequest, Request.Builder authRequestBuilder);

}
