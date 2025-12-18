package com.jbm.cluster.push;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;
import com.alibaba.fastjson.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 企业微信access_token获取测试
 * 文档地址: https://developer.work.weixin.qq.com/document/path/91039
 */
public class WorkWeixinTokenTest {

    private static final Logger logger = LoggerFactory.getLogger(WorkWeixinTokenTest.class);

    // 企业微信获取access_token的接口地址
    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";

    /**
     * 测试获取企业微信access_token
     * 需要替换以下参数为实际的企业微信参数:
     * CORP_ID: 企业ID
     * CORP_SECRET: 应用的凭证密钥
     */
    @Test
    public void testGetAccessToken() throws Exception {
        // TODO: 替换为真实的企业微信参数
        NacosConfigClient nacosConfigClient = new NacosConfigClient("http://10.100.10.62:8848");

        String str = nacosConfigClient.getConfig("WorkWeixin.properties", "DEFAULT_GROUP", "jaja-dev");
        WorkWeixinProperties config = new WorkWeixinProperties();
        SpringPropsBinder.bind(str, config);

        OkHttpClient client = createOkHttpClient();

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

        logger.info("准备发送请求到企业微信获取access_token接口");

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
            Integer expiresIn = jsonResponse.getInteger("expires_in");

            logger.info("成功获取access_token!");
            logger.info("access_token: {}", accessToken);
            logger.info("有效期: {}秒", expiresIn);
        }
    }

    /**
     * 创建支持HTTPS的OkHttpClient实例
     * 注意: 此处使用了信任所有证书的配置, 仅适用于测试环境
     */
    private OkHttpClient createOkHttpClient() {
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