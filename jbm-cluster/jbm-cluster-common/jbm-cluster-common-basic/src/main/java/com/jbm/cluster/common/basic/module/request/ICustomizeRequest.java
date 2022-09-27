package com.jbm.cluster.common.basic.module.request;

import cn.hutool.http.HttpResponse;

public interface ICustomizeRequest {
    HttpResponse request(String url, String methodType, String jsonBody);

    String prefix();
}
