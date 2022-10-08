package com.jbm.cluster.common.basic.module.request;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

public interface ICustomizeRequest {
    HttpResponse request(String url, String methodType, String jsonBody);

    HttpResponse request(HttpRequest httpRequest);

    String prefix();
}
