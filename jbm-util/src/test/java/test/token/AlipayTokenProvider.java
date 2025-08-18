package test.token;

import com.jbm.util.token.TokenException;
import com.jbm.util.token.TokenInfo;
import com.jbm.util.token.TokenProvider;

import java.time.Instant;

// 2. 支付宝平台 Provider
class AlipayTokenProvider implements TokenProvider {
    // ... 类似实现 ...
    @Override
    public TokenInfo getToken() throws TokenException {
        return new TokenInfo("alipay-access-token-456", Instant.now().plusSeconds(3600));
    }

    @Override
    public TokenInfo refreshToken() throws TokenException {
        return getToken();
    }

}
