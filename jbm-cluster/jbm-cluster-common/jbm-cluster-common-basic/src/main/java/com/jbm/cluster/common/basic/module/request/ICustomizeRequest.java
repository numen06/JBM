package com.jbm.cluster.common.basic.module.request;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import java.net.UnknownHostException;

public interface ICustomizeRequest {
    HttpResponse request(String url, String methodType, String jsonBody) throws UnknownHostException;

    HttpResponse request(HttpRequest httpRequest);

    String prefix();


//    void requestAsync(HttpRequest httpRequest);


}
