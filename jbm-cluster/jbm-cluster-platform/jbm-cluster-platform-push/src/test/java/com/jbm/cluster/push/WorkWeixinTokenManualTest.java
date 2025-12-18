package com.jbm.cluster.push;

import okhttp3.*;
import com.alibaba.fastjson.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 企业微信access_token获取手动测试类
 * 可以交互式输入企业微信参数进行测试
 * 文档地址: https://developer.work.weixin.qq.com/document/path/91039
 */
public class WorkWeixinTokenManualTest {

    // 企业微信获取access_token的接口地址
    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== 企业微信access_token获取测试 ===");
        System.out.print("请输入企业ID (corpid): ");
        String corpId = scanner.nextLine().trim();

        System.out.print("请输入应用凭证密钥 (corpsecret): ");
        String corpSecret = scanner.nextLine().trim();

        if (corpId.isEmpty() || corpSecret.isEmpty()) {
            System.err.println("企业ID和应用凭证密钥不能为空!");
            return;
        }

        WorkWeixinTokenManualTest tester = new WorkWeixinTokenManualTest();
        tester.testGetAccessToken(corpId, corpSecret);
    }

    /**
     * 测试获取企业微信access_token
     *
     * @param corpId     企业ID
     * @param corpSecret 应用的凭证密钥
     */
    public void testGetAccessToken(String corpId, String corpSecret) throws Exception {
        OkHttpClient client = createOkHttpClient();

        // 构造请求URL
        HttpUrl url = HttpUrl.parse(TOKEN_URL).newBuilder()
                .addQueryParameter("corpid", corpId)
                .addQueryParameter("corpsecret", corpSecret)
                .build();

        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        System.out.println("正在发送请求到企业微信获取access_token接口...");

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("请求失败, HTTP状态码: " + response.code());
                throw new IOException("请求失败: " + response.code());
            }

            String responseBody = response.body().string();
            System.out.println("接收到响应: " + responseBody);

            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(responseBody);
            Integer errcode = jsonResponse.getInteger("errcode");

            if (errcode != null && errcode != 0) {
                String errmsg = jsonResponse.getString("errmsg");
                System.err.println("获取access_token失败, 错误码: " + errcode + ", 错误信息: " + errmsg);
                throw new RuntimeException("获取access_token失败: " + errmsg);
            }

            String accessToken = jsonResponse.getString("access_token");
            Integer expiresIn = jsonResponse.getInteger("expires_in");

            System.out.println("成功获取access_token!");
            System.out.println("access_token: " + accessToken);
            System.out.println("有效期: " + expiresIn + "秒");
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