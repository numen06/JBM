package com.jbm.test.tio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.common.session.id.ISessionIdGenerator;
import org.tio.http.server.HttpServerStarter;
import org.tio.http.server.handler.DefaultHttpRequestHandler;
import org.tio.http.server.mvc.Routes;
import org.tio.utils.SystemTimer;

import java.io.IOException;

/**
 *
 */
public class TioHttpServerStarter {

    public static HttpConfig httpConfig;
    public static HttpRequestHandler requestHandler;
    public static HttpServerStarter httpServerStarter;
    private static Logger log = LoggerFactory.getLogger(TioHttpServerStarter.class);

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();

        int port = 888;// 启动端口
        String pageRoot = "/";// html/css/js等的根目录，支持classpath:，也支持绝对路径
        String[] scanPackages = new String[]{"com.jbm.test"};

        httpConfig = new HttpConfig(port, null, null, null);
        httpConfig.setPageRoot(pageRoot);
        httpConfig.setSessionIdGenerator(new ISessionIdGenerator() {

            @Override
            public String sessionId(HttpConfig httpConfig, HttpRequest request) {
                return request.getChannelContext().getClientNode().toString();
            }
        });

        Routes routes = new Routes(scanPackages);
        DefaultHttpRequestHandler requestHandler = new DefaultHttpRequestHandler(httpConfig, routes);
        requestHandler.setHttpSessionListener(new TestHttpSessionListener());
        httpServerStarter = new HttpServerStarter(httpConfig, requestHandler);
        httpServerStarter.start();

        long end = SystemTimer.currentTimeMillis();
        long iv = end - start;
        log.info("Tio Http Server启动完毕,耗时:{}ms,访问地址:http://127.0.0.1:{}", iv, port);
    }
}