package com.jbm.cluster.common.basic.module.request;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

public class JbmHttpsRequest extends JbmBaseRequest {
    @Override
    public String prefix() {
        return "https";
    }


}
