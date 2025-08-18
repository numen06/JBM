package test.token;

// 假设你有多个平台的 TokenProvider 实现

import com.jbm.util.token.TokenException;
import com.jbm.util.token.TokenInfo;
import com.jbm.util.token.TokenProvider;

import java.time.Instant;

// 1. 微信平台 Provider
class WeChatTokenProvider implements TokenProvider {
    private final String appId;
    private final String appSecret;

    public WeChatTokenProvider(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    @Override
    public TokenInfo getToken() throws TokenException {
        // 调用微信 API 获取 access_token
        // String token = callWeChatApi(appId, appSecret);
        // return new TokenInfo(token, Instant.now().plusSeconds(7000)); // 微信 token 通常 7200s
        return new TokenInfo("wechat-access-token-123", Instant.now().plusSeconds(7000));
    }

    @Override
    public TokenInfo refreshToken() throws TokenException {
        // 微信 access_token 通常通过 appId/appSecret 重新获取，或使用 refresh_token (如果支持)
        return getToken(); // 简化示例
    }

}