package com.jbm.cluster.platform.gateway.logfilter;

import com.jbm.cluster.api.bus.event.RemoteRefreshRouteEvent;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.api.service.feign.client.BaseApiServiceClient;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ApiFilter 在 {@link com.jbm.cluster.platform.gateway.service.impl.StreamAccessLogService#sendLog}
 * 链路上执行，发生在网关已把下游响应写回客户端之后；此处 Feign 调 center 得到 401（如无效 Token）
 * 只影响访问日志里的 API 元数据补全，<strong>不应</strong>也无法再把对客户端的 HTTP 状态改成 401。
 * <p>
 * 若希望对客户端返回 401，应在请求进入路由<strong>之前</strong>的鉴权过滤器中处理（如 Sa-Token / Token 校验），
 * 而不是在访问日志阶段；服务间调用应使用内部凭证，避免 findApiByPath 因网关 Token 无效而 401。
 */
@ExtendWith(MockitoExtension.class)
class ApiFilterFeign401Test {

    @Mock
    private BaseApiServiceClient baseApiServiceClient;

    @InjectMocks
    private ApiFilter apiFilter;

    @BeforeEach
    void invalidateCache() {
        apiFilter.onApplicationEvent(new RemoteRefreshRouteEvent());
    }

    @Test
    void findApiByPathFeign401_doesNotThrow_andDoesNotSetApiMetadata() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://jbm-cluster-platform-center/api/findApiByPath",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8);
        Response response = Response.builder()
                .status(401)
                .reason("Unauthorized")
                .request(request)
                .body("{\"code\":401,\"message\":\"无效Token\"}", StandardCharsets.UTF_8)
                .build();
        FeignException fe401 = FeignException.errorStatus("GET", response);
        when(baseApiServiceClient.findApiByPath(anyString(), anyString())).thenThrow(fe401);

        GatewayLogInfo logInfo = new GatewayLogInfo();
        logInfo.setServiceId("smart-building-app");
        logInfo.setPath("/smart-building-app/baseProject/selectList");
        Map<String, String> headers = new HashMap<>();

        assertDoesNotThrow(() -> apiFilter.filter(logInfo, headers));
        assertNull(logInfo.getApiId(), "401 时未补全 API 元数据");
    }
}
