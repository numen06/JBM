package com.jbm.cluster.platform.gateway.logfilter;

import com.jbm.cluster.api.bus.event.RemoteRefreshRouteEvent;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.common.mysql.service.BaseApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiFilterServiceLookupFailureTest {

    @Mock
    private BaseApiService baseApiService;

    @InjectMocks
    private ApiFilter apiFilter;

    @BeforeEach
    void invalidateCache() {
        apiFilter.onApplicationEvent(new RemoteRefreshRouteEvent());
    }

    @Test
    void findApiByPathFailure_doesNotThrow_andDoesNotSetApiMetadata() {
        when(baseApiService.findApiByPath(anyString(), anyString()))
                .thenThrow(new RuntimeException("api metadata lookup failed"));

        GatewayLogInfo logInfo = new GatewayLogInfo();
        logInfo.setServiceId("smart-building-app");
        logInfo.setPath("/smart-building-app/baseProject/selectList");
        Map<String, String> headers = new HashMap<>();

        assertDoesNotThrow(() -> apiFilter.filter(logInfo, headers));
        assertNull(logInfo.getApiId(), "API metadata should not be filled when lookup fails");
    }
}
