package test.token;

import com.jbm.util.token.TokenException;
import com.jbm.util.token.TokenInfo;
import com.jbm.util.token.TokenProvider;

import java.time.Instant;

// 3. GitHub 平台 Provider
class GitHubTokenProvider implements TokenProvider {
    private final String personalAccessToken;

    public GitHubTokenProvider(String personalAccessToken) {
        this.personalAccessToken = personalAccessToken;
    }

    @Override
    public TokenInfo getToken() throws TokenException {
        // GitHub Personal Access Token 通常不过期（或长期有效），但这里模拟有有效期
        return new TokenInfo(personalAccessToken, Instant.now().plusSeconds(86400)); // 1 day
    }

    @Override
    public TokenInfo refreshToken() throws TokenException {
        // 对于 PAT，可能不需要刷新，或需要用户重新授权
        throw new TokenException("GitHub Personal Access Token refresh not supported.");
    }

}