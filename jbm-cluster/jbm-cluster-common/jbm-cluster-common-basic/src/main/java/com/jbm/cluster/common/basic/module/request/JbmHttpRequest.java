package com.jbm.cluster.common.basic.module.request;

import cn.hutool.http.HttpRequest;

public class JbmHttpRequest extends JbmBaseRequest {
    @Override
    public String prefix() {
        return "http";
    }

}
