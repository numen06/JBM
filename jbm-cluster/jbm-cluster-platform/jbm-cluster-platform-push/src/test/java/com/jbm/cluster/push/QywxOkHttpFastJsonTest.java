package com.jbm.cluster.push;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jbm.util.nacos.NacosConfigClient;
import jbm.framework.spring.config.SpringPropsBinder;
import jbm.framework.web.WebUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 【JUnit 5 测试类】企业微信动态调用测试（OkHttp + FastJSON）
 * 功能：✅ 获取用户列表 + ✅ 发送文本消息
 * 运行方式：直接执行本测试类（IDE 点绿色箭头 / Maven: mvn test）
 */
@Slf4j
public class QywxOkHttpFastJsonTest {

    // =================================================

    // 🕒 Token 缓存（线程安全）
    private static WorkWeixinProperties workWeixinProperties;

    // 🌐 OkHttp 客户端（复用）
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // 🧹 测试前重置 token 缓存（确保每个 test 独立）
    @BeforeEach
    void resetTokenCache() {
        workWeixinProperties = new WorkWeixinProperties();
        // 从配置中获取企业微信参数
        NacosConfigClient nacosConfigClient = new NacosConfigClient("http://10.100.10.62:8848");
        String str = nacosConfigClient.getConfig("WorkWeixin.properties", "jaja-dev");
        SpringPropsBinder.fill(str, workWeixinProperties);
    }

    // ✅ Test 1：获取 access_token 并验证格式
    @Test
    @DisplayName("✅ 测试1：获取 access_token")
    void shouldGetAccessTokenSuccessfully() throws Exception {
        String token = getAccessToken();
        assertNotNull(token, "access_token 不应为 null");
        assertTrue(token.length() > 20, "access_token 长度应大于 20 字符");
        System.out.println("✅ access_token 获取成功：" + token.substring(0, 20) + "...");
    }

    // ✅ Test 2：获取用户列表（至少返回 1 个用户）
    @Test
    @DisplayName("✅ 测试2：获取用户列表（根部门）")
    void shouldListUsersFromDepartment1() throws Exception {
        String token = getAccessToken();
        List<String> userIds = listSimpleUsers(token);
        assertNotNull(userIds, "userIds 列表不应为 null");
        assertTrue(userIds.size() >= 1, "应至少获取到 1 个用户（当前：" + userIds.size() + "）");
        System.out.println("👥 获取到 " + userIds.size() + " 个用户（示例：" +
                userIds.subList(0, Math.min(3, userIds.size())) + ")");
    }

    // ✅ Test 3：发送消息给第一个用户（核心业务链路）
    @Test
    @DisplayName("✅ 测试3：发送文本消息给首个用户")
    void shouldSendTextMessageToFirstUser() throws Exception {
        String token = getAccessToken();
        List<String> userIds = listSimpleUsers(token);
        assertNotEquals(0, userIds.size(), "用户列表为空，无法发送消息");

        String firstUserId = "@all";
        boolean success = sendTextMessage(token, firstUserId,
                "Hello from OkHttp + FastJSON! ✅\n" +
                        "⏱️ 时间：" + Instant.now());

        assertTrue(success, "消息发送应成功");
        System.out.println("📤 已向用户 '" + firstUserId + "' 发送测试消息 ✅");
    }

    // ✅ Test 4：获取部门列表
    @Test
    @DisplayName("✅ 测试4：获取部门列表")
    void shouldGetDepartmentList() throws Exception {
        String token = getAccessToken();
        JSONArray departments = listDepartments(token);
        assertNotNull(departments, "部门列表不应为 null");
        for (int i = 0; i < departments.size(); i++) {
            JSONObject department = departments.getJSONObject(i);
            log.info("部门：{}", department);
        }
    }

    // ✅ Test 5：发送图片消息给指定用户
    @Test
    @DisplayName("✅ 测试5：发送图片消息给指定用户")
    void shouldSendImageMessageToUser() throws Exception {
        String token = getAccessToken();
        List<String> userIds = listSimpleUsers(token);
        assertNotEquals(0, userIds.size(), "用户列表为空，无法发送消息");

        // 注意：需要先上传图片素材到企业微信，获取media_id
        // 这里使用一个示例media_id，实际使用时需要替换成有效的media_id
        String mediaId = this.shouldUploadMediaFile();

        String firstUserId = "zhanglie8904";
        boolean success = sendImageMessage(token, firstUserId, mediaId);

        assertTrue(success, "图片消息发送应成功");
        System.out.println("📤 已向用户 '" + firstUserId + "' 发送图片消息 ✅");
    }

    // ✅ Test 6：上传文件素材
    @DisplayName("✅ 测试6：上传文件素材")
    String shouldUploadMediaFile() throws Exception {
        String token = getAccessToken();

        // 注意：需要指定一个真实存在的文件路径
        // 这里使用一个示例文件路径，实际使用时需要替换成有效的文件路径
        String url = "https://hao123-static.cdn.bcebos.com/cms/2025-11/1762919100613/754ecffb44b7.png";
        //下载图片
        String filePath = "../../../data/test-upload.png";
        File dirImageFile = new File(filePath);
        HttpUtil.downloadFile(url, dirImageFile);
        String mediaType = "image";
        JSONObject result = uploadMedia(token, mediaType, dirImageFile.getAbsolutePath());

        assertNotNull(result, "上传结果不应为null");
        assertEquals("0", result.getString("errcode"), "上传应成功");
        assertTrue(result.containsKey("media_id"), "返回结果应包含media_id");

        System.out.println("📤 文件上传成功，media_id: " + result.getString("media_id"));
        return result.getString("media_id");
    }

    // ———————————————————————— 核心工具方法（同上，仅去除了 main） ————————————————————————
    // 企业微信获取access_token的接口地址
    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String TOKEN_PATH = "../../../data/access_token.json";

    private static String getAccessToken() throws Exception {
        File file = new File(TOKEN_PATH);
        //如果有临时文件没有过期则返回"access_token.json",读取文件后判断是否过期
        if (FileUtil.exist(file)) {
            String json = FileUtil.readUtf8String(file);
            JSONObject jsonResponse = JSONObject.parseObject(json);
            Integer expiresIn = jsonResponse.getInteger("expires_in");
            String accessToken = jsonResponse.getString("access_token");
            // 根据文件创建时间计算过期时间,往前推1个小时安全时间
            Date createTime = FileUtil.lastModifiedTime(file);
            long expireTime = createTime.getTime() + expiresIn * 1000;
            if (System.currentTimeMillis() < expireTime - 60 * 60 * 1000  ) {
                log.info("access_token未过期，直接返回");
                return accessToken;
            }
            log.info("access_token已过期，重新获取");
        }
        // 构造请求URL
        HttpUrl url = HttpUrl.parse(TOKEN_URL).newBuilder()
                .addQueryParameter("corpid", workWeixinProperties.getCorpId())
                .addQueryParameter("corpsecret", workWeixinProperties.getCorpSecret())
                .build();

        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        log.info("准备获取access_token");

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("请求失败, HTTP状态码: {}", response.code());
                throw new RuntimeException("请求失败: " + response.code());
            }

            String responseBody = response.body().string();
            log.info("接收到响应: {}", responseBody);

            // 解析响应
            JSONObject jsonResponse = JSONObject.parseObject(responseBody);
            Integer errcode = jsonResponse.getInteger("errcode");

            if (errcode != null && errcode != 0) {
                String errmsg = jsonResponse.getString("errmsg");
                log.error("获取access_token失败, 错误码: {}, 错误信息: {}", errcode, errmsg);
                throw new RuntimeException("获取access_token失败: " + errmsg);
            }

            String accessToken = jsonResponse.getString("access_token");
            Integer expiresIn = jsonResponse.getInteger("expires_in");
            log.info("成功获取access_token!");
            log.info("access_token: {}", accessToken);
            log.info("有效期: {}秒", expiresIn);
            // 存在本地临时文件
            FileUtil.writeUtf8String(jsonResponse.toJSONString(),file);
            return accessToken;
        }
    }


    private List<String> listSimpleUsers(String token) throws IOException {
        // 使用HttpUrl.Builder构造URL
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("qyapi.weixin.qq.com")
                .addPathSegment("cgi-bin")
                .addPathSegment("user")
                .addPathSegment("simplelist")
                .addQueryParameter("access_token", token)
                .addQueryParameter("department_id", "32")
                .addQueryParameter("fetch_child", "1")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }
            String body = response.body().string();
            JSONObject json = JSON.parseObject(body);
            String errcode = json.getString("errcode");
            if (!"0".equals(errcode)) {
                throw new IOException("获取用户列表失败: " + json.getString("errmsg"));
            }
            JSONArray userList = json.getJSONArray("userlist");
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < userList.size(); i++) {
                JSONObject u = userList.getJSONObject(i);
                log.info("用户: " + u.getString("name") + "(" + u.getString("userid") + ")");
                ids.add(u.getString("userid"));
            }
            return ids;
        }
    }

    private boolean sendTextMessage(String token, String userId, String content) throws IOException {
        // 使用HttpUrl.Builder构造URL
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("qyapi.weixin.qq.com")
                .addPathSegment("cgi-bin")
                .addPathSegment("message")
                .addPathSegment("send")
                .addQueryParameter("access_token", token)
                .build();

        JSONObject msg = new JSONObject();
        msg.put("touser", userId);
        msg.put("msgtype", "text");
        msg.put("agentid", workWeixinProperties.getAgentId());
        msg.put("text", new JSONObject().fluentPut("content", content));
        msg.put("safe", 0);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                msg.toJSONString()
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("HTTP Error: " + response.code() + " " + response.message());
                return false;
            }
            String responseBody = response.body().string();
            JSONObject json = JSON.parseObject(responseBody);
            String errcode = json.getString("errcode");
            if ("0".equals(errcode)) {
                return true;
            } else {
                System.err.println("发送失败: " + json.getString("errmsg") + " (errcode=" + errcode + ")");
                return false;
            }
        }
    }

    /**
     * 获取部门列表
     * 文档地址：https://developer.work.weixin.qq.com/document/path/90208
     *
     * @param token 有效的access_token
     * @return 部门列表JSONArray
     * @throws IOException 网络请求异常
     */
    private JSONArray listDepartments(String token) throws IOException {
        // 使用HttpUrl.Builder构造URL
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("qyapi.weixin.qq.com")
                .addPathSegment("cgi-bin")
                .addPathSegment("department")
                .addPathSegment("list")
                .addQueryParameter("access_token", token)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }
            String body = response.body().string();
            JSONObject json = JSON.parseObject(body);
            String errcode = json.getString("errcode");
            if (!"0".equals(errcode)) {
                throw new IOException("获取部门列表失败: " + json.getString("errmsg"));
            }
            return json.getJSONArray("department");
        }
    }

    /**
     * 发送图片消息
     * 文档地址：https://developer.work.weixin.qq.com/document/path/90236#图片消息
     *
     * @param token   有效的access_token
     * @param userId  用户ID
     * @param mediaId 图片的media_id
     * @return 是否发送成功
     * @throws IOException 网络请求异常
     */
    private boolean sendImageMessage(String token, String userId, String mediaId) throws IOException {
        // 使用HttpUrl.Builder构造URL
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("qyapi.weixin.qq.com")
                .addPathSegment("cgi-bin")
                .addPathSegment("message")
                .addPathSegment("send")
                .addQueryParameter("access_token", token)
                .build();

        // 构造图片消息体
        JSONObject imageMsg = new JSONObject();
        imageMsg.put("touser", userId);
        imageMsg.put("msgtype", "image");
        imageMsg.put("agentid", workWeixinProperties.getAgentId());

        JSONObject image = new JSONObject();
        image.put("media_id", mediaId);
        imageMsg.put("image", image);

        imageMsg.put("safe", 0);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                imageMsg.toJSONString()
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("HTTP Error: " + response.code() + " " + response.message());
                return false;
            }
            String responseBody = response.body().string();
            JSONObject json = JSON.parseObject(responseBody);
            String errcode = json.getString("errcode");
            if ("0".equals(errcode)) {
                return true;
            } else {
                System.err.println("发送失败: " + json.getString("errmsg") + " (errcode=" + errcode + ")");
                return false;
            }
        }
    }

    /**
     * 上传临时素材
     * 文档地址：https://developer.work.weixin.qq.com/document/path/90253
     *
     * @param token 有效的access_token
     * @param type  媒体文件类型，分别有图片（image）、语音（voice）、视频（video），普通文件（file）
     * @param filePath 文件路径
     * @return 上传结果JSONObject
     * @throws IOException 网络请求异常
     */
    private JSONObject uploadMedia(String token, String type, String filePath) throws IOException {
        // 使用HttpUrl.Builder构造URL
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("qyapi.weixin.qq.com")
                .addPathSegment("cgi-bin")
                .addPathSegment("media")
                .addPathSegment("upload")
                .addQueryParameter("access_token", token)
                .addQueryParameter("type", type)
                .build();

        // 构造文件请求体
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }

        RequestBody fileBody = RequestBody.create(
                MediaType.parse("application/octet-stream"),
                file);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("media", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }

            String responseBody = response.body().string();
            return JSON.parseObject(responseBody);
        }
    }
}