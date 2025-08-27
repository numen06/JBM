package com.jbm.cluster.common.basic.module.request;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import java.net.UnknownHostException;

public class JbmHttpsRequest extends JbmBaseRequest {
    @Override
    public String prefix() {
        return "https";
    }

}
