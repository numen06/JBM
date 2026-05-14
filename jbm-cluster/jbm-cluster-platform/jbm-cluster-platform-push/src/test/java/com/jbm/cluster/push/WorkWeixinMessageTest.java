package com.jbm.cluster.push;

import com.alibaba.fastjson.JSONObject;
import com.jbm.util.nacos.NacosConfigClient;
import jbm.framework.spring.config.SpringPropsBinder;
import okhttp3.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * 企业微信发送应用消息测试
 * 文档地址: https://developer.work.weixin.qq.com/document/path/90236
 */
public class WorkWeixinMessageTest {

    private static final Logger logger = LoggerFactory.getLogger(WorkWeixinMessageTest.class);

    // 企业微信发送应用消息的接口地址
    private static final String MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send";

    // 企业微信获取access_token的接口地址
    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    
    private static String accessToken;
    
    private static OkHttpClient client;

    @BeforeAll
    public static void setUp() throws Exception {
        client = createOkHttpClient();
        accessToken = getAccessToken();
    }

    /**
     * 测试发送文本消息到企业微信
     */
    @Test
    public void testSendTextMessage() throws Exception {
        // 构造请求URL，添加access_token参数
        HttpUrl url = HttpUrl.parse(MESSAGE_URL).newBuilder()
                .addQueryParameter("access_token", accessToken)
                .build();

        // 构造请求体数据
        JSONObject requestBody = new JSONObject();
        requestBody.put("touser", "@all"); // 发送给所有人
        requestBody.put("msgtype", "text");
        requestBody.put("agentid", 1000002); // 需要替换为实际的应用ID
        
        JSONObject textContent = new JSONObject();
        textContent.put("content", "这是一条测试消息");
        requestBody.put("text", textContent);
        
        requestBody.put("duplicate_check_interval", 1800);

        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody.toJSONString(), MediaType.get("application/json;charset=utf-8")))
                .build();

        logger.info("准备发送文本消息到企业微信");

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("请求失败, HTTP状态码: {}", response.code());
                throw new RuntimeException("请求失败: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("接收到响应: {}", responseBody);

            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(responseBody);
            Integer errcode = jsonResponse.getInteger("errcode");

            if (errcode != null && errcode != 0) {
                String errmsg = jsonResponse.getString("errmsg");
                logger.error("发送消息失败, 错误码: {}, 错误信息: {}", errcode, errmsg);
                throw new RuntimeException("发送消息失败: " + errmsg);
            }

            logger.info("成功发送消息!");
        }
    }

    /**
     * 获取企业微信access_token
     */
    private static String getAccessToken() throws Exception {
        // 从配置中获取企业微信参数
        NacosConfigClient nacosConfigClient = new NacosConfigClient("http://10.100.10.62:8848");
        String str = nacosConfigClient.getConfig("WorkWeixin.properties", "DEFAULT_GROUP", "jaja-dev");
        WorkWeixinProperties config = new WorkWeixinProperties();
        SpringPropsBinder.fill(str, config);

        // 构造请求URL
        HttpUrl url = HttpUrl.parse(TOKEN_URL).newBuilder()
                .addQueryParameter("corpid", config.getCorpId())
                .addQueryParameter("corpsecret", config.getCorpSecret())
                .build();

        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        logger.info("准备获取access_token");

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("请求失败, HTTP状态码: {}", response.code());
                throw new RuntimeException("请求失败: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("接收到响应: {}", responseBody);

            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(responseBody);
            Integer errcode = jsonResponse.getInteger("errcode");

            if (errcode != null && errcode != 0) {
                String errmsg = jsonResponse.getString("errmsg");
                logger.error("获取access_token失败, 错误码: {}, 错误信息: {}", errcode, errmsg);
                throw new RuntimeException("获取access_token失败: " + errmsg);
            }

            String accessToken = jsonResponse.getString("access_token");
            logger.info("成功获取access_token: {}", accessToken);
            return accessToken;
        }
    }

    /**
     * 创建支持HTTPS的OkHttpClient实例
     * 注意: 此处使用了信任所有证书的配置, 仅适用于测试环境
     */
    private static OkHttpClient createOkHttpClient() {
        try {
            // 创建信任所有证书的TrustManager (仅用于测试!)
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // 初始化SSL上下文
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建SSL套接字工厂
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // 构建OkHttpClient
            return new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true) // 信任所有主机名 (仅用于测试!)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("创建OkHttpClient失败", e);
        }
    }
}