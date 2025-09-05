package com.jbm.cluster.common.basic.module.request;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import java.net.UnknownHostException;

public interface ICustomizeRequest {
    okhttp3.Response request(String url, String methodType, String jsonBody) ;

    okhttp3.Response request(okhttp3.Request httpRequest);

    String prefix();


//    void requestAsync(HttpRequest httpRequest);


}
