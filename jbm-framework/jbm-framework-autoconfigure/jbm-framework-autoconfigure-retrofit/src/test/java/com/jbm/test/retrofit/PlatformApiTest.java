package com.jbm.test.retrofit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(classes = {Application.class})
public class PlatformApiTest {

    private final WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));

    @Autowired
    private PlatformAApi platformAApi;

    @Autowired
    private PlatformBApi platformBApi;

    @BeforeEach
    public void setUp() {
        wireMockServer.start();
        WireMock.configureFor("0.0.0.0", 8089);
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void testPlatformA() {
        // 模拟 PlatformA 的响应
        wireMockServer.stubFor(get(urlEqualTo("/data"))
                .withHeader("Authorization", equalTo("Bearer correct-signature-platform-a"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"platform\":\"A\", \"message\":\"Hello from Platform A\"}")));
        // 调用接口
        String response2 = platformAApi.getData("Bearer correct-signature-platform-a");

        // 验证响应
        assertEquals("{\"platform\":\"A\", \"message\":\"Hello from Platform A\"}", response2);
    }

    @Test
    public void testPlatformB() {
        wireMockServer.stubFor(get(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("test-token")));
        String token = platformBApi.token();

        // 模拟 PlatformB 的响应
        stubFor(post(urlEqualTo("/create"))
                .withHeader("Authorization", equalTo("Bearer " + token))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"platform\":\"B\", \"message\":\"Hello from Platform B\"}")));

        // 调用接口
        String response = platformBApi.create( "wo shi body");

        // 验证响应
        assertEquals("{\"platform\":\"B\", \"message\":\"Hello from Platform B\"}", response);
    }
}