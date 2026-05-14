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
 * 企业微信获取成员ID列表测试
 * 文档地址: https://developer.work.weixin.qq.com/document/path/96067
 */
public class WorkWeixinMemberTest {

    private static final Logger logger = LoggerFactory.getLogger(WorkWeixinMemberTest.class);

    // 企业微信获取成员ID列表的接口地址
    private static final String MEMBER_LIST_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/list_id";
    
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
     * 测试获取企业微信成员ID列表
     */
    @Test
    public void testGetMemberList() throws Exception {
        // 构造请求URL，添加access_token参数
        HttpUrl url = HttpUrl.parse(MEMBER_LIST_URL).newBuilder()
                .addQueryParameter("access_token", accessToken)
                .build();

        // 构造请求体数据
        JSONObject requestBody = new JSONObject();
        // 可以添加cursor和limit参数进行分页
        // requestBody.put("cursor", "xxxxxx");
        // requestBody.put("limit", 10000);

        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody.toJSONString(), MediaType.get("application/json;charset=utf-8")))
                .build();

        logger.info("准备获取企业微信成员ID列表");

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
                logger.error("获取成员ID列表失败, 错误码: {}, 错误信息: {}", errcode, errmsg);
                throw new RuntimeException("获取成员ID列表失败: " + errmsg);
            }

            logger.info("成功获取成员ID列表!");
            
            // 输出部分结果示例
            String nextCursor = jsonResponse.getString("next_cursor");
            logger.info("下次游标: {}", nextCursor);
            
            // 如果需要处理具体的用户列表数据，可以在这里添加代码
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