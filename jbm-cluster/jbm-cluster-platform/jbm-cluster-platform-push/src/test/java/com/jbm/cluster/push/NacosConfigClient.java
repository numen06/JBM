package com.jbm.cluster.push;

import okhttp3.*;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class NacosConfigClient {

    private final OkHttpClient httpClient;
    private final String serverAddr; // e.g., "http://127.0.0.1:8848"
    private final String username;
    private final String password;

    public NacosConfigClient(String serverAddr) {
        this(serverAddr, null, null);
    }

    public NacosConfigClient(String serverAddr, String username, String password) {
        this.serverAddr = serverAddr.endsWith("/") ? serverAddr.substring(0, serverAddr.length() - 1) : serverAddr;
        this.username = username;
        this.password = password;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS);

        // 若启用了基础认证，添加拦截器
        if (username != null && password != null) {
            builder.addInterceptor(new BasicAuthInterceptor(username, password));
        }

        this.httpClient = builder.build();
    }

    /**
     * 获取 Nacos 配置（同步阻塞）
     *
     * @param dataId   配置 ID，如 "app.yaml"
     * @param group    分组，默认 "DEFAULT_GROUP"
     * @param tenant   命名空间 ID（即 namespaceId），可为 null/"" 表示 public
     * @return 配置内容（原始字符串，如 YAML/Properties 内容），失败返回 null
     */
    public String getConfig(String dataId, String group, String tenant) {
        if (dataId == null || dataId.trim().isEmpty()) {
            throw new IllegalArgumentException("dataId cannot be null or empty");
        }
        if (group == null || group.trim().isEmpty()) {
            group = "DEFAULT_GROUP";
        }

        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(getHostFromAddr())
                .port(getPortFromAddr())
                .addPathSegments("nacos/v1/cs/configs")
                .addQueryParameter("dataId", dataId)
                .addQueryParameter("group", group)
                .addQueryParameter("tenant", Objects.toString(tenant, ""))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                System.err.println("Failed to fetch config: " + response.code() + " " + response.message());
                return null;
            }
        } catch (IOException e) {
            System.err.println("IOException while fetching config: " + e.getMessage());
            return null;
        }
    }


    /**
     * 查询指定命名空间下的所有配置
     *
     * @param tenant   命名空间 ID（即 namespaceId），可为 null/"" 表示 public
     * @param pageNo   页码，默认为1
     * @param pageSize 每页大小，默认为100
     * @return 配置列表的JSON字符串，失败返回 null
     */
    public String listConfigsInTenant(String tenant, Integer pageNo, Integer pageSize) {
        if (pageNo == null) pageNo = 1;
        if (pageSize == null) pageSize = 100;

        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme("http")
                .host(getHostFromAddr())
                .port(getPortFromAddr())
                .addPathSegments("nacos/v1/cs/configs")
                .addQueryParameter("pageNo", String.valueOf(pageNo))
                .addQueryParameter("pageSize", String.valueOf(pageSize));

        // 只有当tenant不为空时才添加tenant参数
        if (tenant != null && !tenant.isEmpty()) {
            urlBuilder.addQueryParameter("tenant", tenant);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                System.err.println("Failed to list configs: " + response.code() + " " + response.message());
                return null;
            }
        } catch (IOException e) {
            System.err.println("IOException while listing configs: " + e.getMessage());
            return null;
        }
    }

    /**
     * 查询指定命名空间下的所有配置（默认分页参数）
     *
     * @param tenant 命名空间 ID（即 namespaceId），可为 null/"" 表示 public
     * @return 配置列表的JSON字符串，失败返回 null
     */
    public String listConfigsInTenant(String tenant) {
        return listConfigsInTenant(tenant, null, null);
    }

    // 辅助方法：提取 host（兼容 http://xxx 或 xxx）
    private String getHostFromAddr() {
        String s = serverAddr.replaceAll("^https?://", "");
        return s.split(":")[0].split("/")[0];
    }

    private int getPortFromAddr() {
        try {
            String s = serverAddr.replaceAll("^https?://", "");
            String[] parts = s.split(":");
            if (parts.length > 1) {
                String portPart = parts[1].split("/")[0];
                return Integer.parseInt(portPart);
            }
        } catch (Exception ignored) {
        }
        return 8848; // default
    }

    // 基础认证拦截器
    static class BasicAuthInterceptor implements Interceptor {
        private final String credentials;

        BasicAuthInterceptor(String user, String password) {
            this.credentials = Credentials.basic(user, password);
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request authenticated = original.newBuilder()
                    .header("Authorization", credentials)
                    .build();
            return chain.proceed(authenticated);
        }
    }

}