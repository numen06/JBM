package com.jbm.cluster.push;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpInterceptor;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Slf4j
public class HttpTest {


    ExecutorService executorService = ThreadUtil.newExecutor(100);
    @Test
    public void testHttp() {

        HttpUtil.createServer(8888)
                .addAction("/", (req, res) -> {
                    Integer r = RandomUtil.randomInt(1, 5);
                    ThreadUtil.safeSleep(1000 * r);
                    res.write("Hello Hutool Server :" + req.getParam("i"));

                })
                .start();
        for (int i = 0; i < 100; i++) {

            final int f = i;

            Future<HttpResponse> future = executorService.submit(new Callable<HttpResponse>() {
                @Override
                public HttpResponse call() throws Exception {
                    HttpRequest httpRequest = new HttpRequest("http://127.0.0.1:8888").body("i=" + f);
                    httpRequest.addRequestInterceptor(new HttpInterceptor<HttpRequest>() {
                        @Override
                        public void process(HttpRequest httpObj) {
                            log.info("start :" + f);

                        }
                    });
                    httpRequest.addResponseInterceptor(new HttpInterceptor<HttpResponse>() {
                        @Override
                        public void process(HttpResponse httpObj) {
                            log.info("end,{}", httpObj.body());
                        }
                    });
                   return httpRequest.executeAsync();
                }
            });
        }
        ThreadUtil.safeSleep(10000);


    }
}
