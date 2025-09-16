package jbm.framework.boot.autoconfigure.emqx;


import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.jbm.util.PageUtils;
import jbm.framework.boot.autoconfigure.emqx.configuration.EmqxProperties;
import jbm.framework.boot.autoconfigure.emqx.model.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author wesley
 */
@Service
@Slf4j
public class EmqxApiService {

    private final EmqxProperties emqxProperties;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
            .build();

    public EmqxApiService(EmqxProperties emqxProperties) {
        this.emqxProperties = emqxProperties;
        if (StrUtil.isEmpty(emqxProperties.getUrl())) {
            throw new RuntimeException("请配置 EMQX 的 管理URL");
        }
        if (StrUtil.isEmpty(emqxProperties.getUsername()) || StrUtil.isEmpty(emqxProperties.getPassword())) {
            throw new RuntimeException("请配置 EMQX 的用户名和密码");
        }
    }

    private Request.Builder builder() {
        return new Request.Builder()
                .header("Content-Type", "application/json")
                .header("Authorization", Credentials.basic(emqxProperties.getUsername(), emqxProperties.getPassword()));
    }

    private String checkBody(Response response) throws IOException {
        if (!response.isSuccessful()) {
            String msg = StrUtil.format("请求[{}][{}]失败：{}", response.request().method(), response.request().url(), response.message());
//            log.error(msg);
            throw new IOException(msg);
        }
        //        if (body.isEmpty()) {
//            log.warn("EMQX 响应体为空");
//        }
        return response.body() != null ? response.body().string() : "";
    }

    public List<AuthenticatorResult> getAuthentication() {
        String url = UrlBuilder.of(emqxProperties.getUrl()).addPath("authentication")
                .build();
        Request request = builder().url(url).get().build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = checkBody(response);
            return JSONObject.parseArray(responseBody, AuthenticatorResult.class);
        } catch (IOException e) {
            log.error("获取 EMQX 认证信息异常", e);
        }
        return null;
    }

    private final MediaType JSON_MEDIA_TYPE = MediaType.Companion.parse("application/json");



    public List<AuthUser> getAllAuthUser(String authId) {
        EmqxClient emqxClient = new EmqxClient();
        emqxClient.setConnected(true);
        List<AuthUser> allAuthUsers = new ArrayList<>();
        PageUtils.loopPage(allAuthUsers, (page) -> getAuthUserPage(authId, page), 1, 100);
        return allAuthUsers;
    }

    public PageResult<AuthUser> getAuthUserPage(String authId, Page  page) {
        String url = UrlBuilder.of(emqxProperties.getUrl()).addPath("/authentication/")
                .addPathSegment(authId)
                .addPath("/users")
                .build();
        Request request = builder().url(url).get().build();
        PageResult<AuthUser> result = new PageResult<>();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = checkBody(response);
            EmqxListResponse<AuthUser> emqxResponse =
                    JSONObject.parseObject(responseBody,
                            new TypeReference<EmqxListResponse<AuthUser>>() {
                            });
            return resultToPage(emqxResponse, page, result);
        } catch (IOException e) {
            log.error("获取 EMQX 认证信息异常", e);
        }
        return result;
    }

    public boolean registerUser(String authId, String clientId, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", clientId);
        body.put("password", StrUtil.nullToEmpty(password));
        String url = UrlBuilder.of(emqxProperties.getUrl()).addPath("/authentication/").addPathSegment(authId).addPath("/users").build();
        RequestBody requestBody = RequestBody.create(JSONObject.toJSONString(body), JSON_MEDIA_TYPE);
        Request request = builder().url(url).post(requestBody).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = checkBody(response);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean unregisterUser(String authId, String clientId) {
        String url = UrlBuilder.of(emqxProperties.getUrl()).addPath("/authentication/")
                .addPathSegment(authId)
                .addPath("/users")
                .addPathSegment(clientId)
                .build();
        Request request = builder().url(url).delete().build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = checkBody(response);
            log.debug("成功删除 EMQX 用户 {}", clientId);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public EmqxClient getClientById(String clientId) {
        String url = UrlBuilder.of(this.emqxProperties.getUrl()).addPath("clients")
                .addPathSegment(clientId)
                .build();
        Request request = builder().url(url).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = checkBody(response);
            return JSONObject.parseObject(responseBody, EmqxClient.class);
        } catch (IOException e) {
            log.error("获取 EMQX 在线客户端列表异常", e);
        }
        return null;
    }

    public List<EmqxClient> getOnlineAllClients() {
        EmqxClient emqxClient = new EmqxClient();
        emqxClient.setConnected(true);
        List<EmqxClient> allEmqxClients = new ArrayList<>();
        PageUtils.loopPage(allEmqxClients, (page) -> selectClients(emqxClient, page), 1, 100);
        return allEmqxClients;
    }

    public PageResult<EmqxClient> selectClients(EmqxClient emqxClient, Page page) {
        UrlBuilder urlBuilder = UrlBuilder.of(emqxProperties.getUrl()).addPath("clients")
                .addQuery("limit", page.getPageSize())
                .addQuery("page", page.getPageNumber());
        if (StrUtil.isNotBlank(emqxClient.getClientId())) {
            urlBuilder.addQuery("like_clientid", emqxClient.getClientId());
        }
        if (emqxClient.getConnected() != null) {
            urlBuilder.addQuery("conn_state", BooleanUtil.isTrue(emqxClient.getConnected()) ? "connected" : "disconnected");
        }
        String url = urlBuilder.build();
        Request request = builder().url(url).get().build();
        PageResult<EmqxClient> result = new PageResult<>(page.getPageNumber(), page.getPageSize());
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = checkBody(response);
            EmqxListResponse<EmqxClient> emqxResponse = JSONObject.parseObject(responseBody, new TypeReference<EmqxListResponse<EmqxClient>>() {
            }.getType());
            log.debug("成功从 EMQX 获取 {} 个在线客户端", emqxResponse.getData().size());
            return resultToPage(emqxResponse, page, result);
        } catch (IOException e) {
            log.error("获取 EMQX 在线客户端列表异常", e);
        }
        return result;
    }

    public List<EmqxSubscription> getSubscriptionsByClient(String clientId) {
        String url = UrlBuilder.of(emqxProperties.getUrl()).addPath("clients/").addPathSegment(clientId).addPath("/subscriptions").build();
        Request request = builder().url(url).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = checkBody(response);
            return JSONObject.parseArray(responseBody, EmqxSubscription.class);
        } catch (IOException e) {
            log.error("获取 EMQX 在线客户端列表异常", e);
        }
        return Collections.emptyList();
    }

    public PageResult<EmqxSubscription> getSubscriptionsByTopic(String topic, Page page) {
        String url = UrlBuilder.of(emqxProperties.getUrl()).addPath("subscriptions/")
                .addQuery("match_topic", topic)
                .addQuery("page", page.getPageNumber())
                .addQuery("limit", page.getPageSize())
                .build();
        Request request = builder().url(url).get().build();
        PageResult<EmqxSubscription> result = new PageResult<>(page.getPageNumber(), page.getPageSize());
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = checkBody(response);
            EmqxListResponse<EmqxSubscription> emqxResponse =
                    JSONObject.parseObject(responseBody,
                            new TypeReference<EmqxListResponse<EmqxSubscription>>() {
                            });
            return resultToPage(emqxResponse, page, result);
        } catch (IOException e) {
            log.error("获取 EMQX 在线客户端列表异常", e);
        }
        return result;
    }

    //内部方法

    private <T> PageResult<T> resultToPage(EmqxListResponse<T> emqxResponse, Page page, PageResult<T> result) {
        List<T> list = emqxResponse.getData() != null ? emqxResponse.getData() : Collections.emptyList();
        return PageUtils.pageResult(page, list, emqxResponse.getMeta().getCount());
    }


}