package test.token;

import com.jbm.util.token.TokenManager;
import com.jbm.util.token.TokenManagerRegistry;
import com.jbm.util.token.TokenProvider;
import org.junit.jupiter.api.Test;

public class MultiPlatformTokenExample {

    @Test
    public void main() {
        TokenManagerRegistry registry = TokenManagerRegistry.getInstance();

        // 1. 为各个平台创建 TokenProvider 和 TokenManager
        TokenProvider wechatProvider = new WeChatTokenProvider("wx_app_123", "wx_secret_456");
        TokenManager wechatManager = new TokenManager(wechatProvider, 60); // 60秒缓冲

        TokenProvider alipayProvider = new AlipayTokenProvider();
        TokenManager alipayManager = new TokenManager(alipayProvider, 60);

        TokenProvider githubProvider = new GitHubTokenProvider("ghp_abc123xyz");
        TokenManager githubManager = new TokenManager(githubProvider, 300); // 5分钟缓冲

        // 2. 注册到 Registry
        registry.registerTokenManager("wechat", wechatManager);
        registry.registerTokenManager("alipay", alipayManager);
        registry.registerTokenManager("github", githubManager);


    }
}
