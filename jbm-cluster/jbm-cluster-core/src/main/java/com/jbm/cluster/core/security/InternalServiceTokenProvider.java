package com.jbm.cluster.core.security;

import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.core.constant.JbmTokenConstants;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OAuth2 Client Credentials service token provider for internal calls.
 */
public final class InternalServiceTokenProvider {

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EXPIRES_IN_PATTERN = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");

    private static volatile String cachedToken;
    private static volatile long expiresAtMillis;

    private InternalServiceTokenProvider() {
    }

    public static String authorizationHeader() {
        String token = token();
        return token == null || token.trim().isEmpty() ? null : JbmTokenConstants.PREFIX + token;
    }

    public static String token() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < expiresAtMillis - 60_000L) {
            return cachedToken;
        }
        synchronized (InternalServiceTokenProvider.class) {
            now = System.currentTimeMillis();
            if (cachedToken != null && now < expiresAtMillis - 60_000L) {
                return cachedToken;
            }
            TokenResponse response = fetchToken();
            cachedToken = response.accessToken;
            expiresAtMillis = now + response.expiresInSeconds * 1000L;
            return cachedToken;
        }
    }

    private static TokenResponse fetchToken() {
        String tokenUrl = config("JBM_INTERNAL_OAUTH_TOKEN_URL", "jbm.internal.oauth.token-url",
                "http://jbm-cluster-platform-auth:5555/oauth2/token");
        String clientId = config("JBM_INTERNAL_OAUTH_CLIENT_ID", "jbm.internal.oauth.client-id",
                JbmConstants.JBM_APP_API_KEY);
        String clientSecret = config("JBM_INTERNAL_OAUTH_CLIENT_SECRET", "jbm.internal.oauth.client-secret",
                JbmConstants.JBM_APP_SECRET);
        String scope = config("JBM_INTERNAL_OAUTH_SCOPE", "jbm.internal.oauth.scope", "internal");
        try {
            String body = "grant_type=client_credentials"
                    + "&client_id=" + encode(clientId)
                    + "&client_secret=" + encode(clientSecret)
                    + "&scope=" + encode(scope);
            HttpURLConnection connection = (HttpURLConnection) new URL(tokenUrl).openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload);
            }
            int status = connection.getResponseCode();
            String response = read(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (status >= 400) {
                throw new IllegalStateException("token endpoint returned " + status + ": " + response);
            }
            String accessToken = match(ACCESS_TOKEN_PATTERN, response);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new IllegalStateException("token endpoint response missing access_token");
            }
            String expires = match(EXPIRES_IN_PATTERN, response);
            long expiresIn = expires == null ? 7200L : Long.parseLong(expires);
            return new TokenResponse(accessToken, Math.max(expiresIn, 300L));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch OAuth2 internal service token", e);
        }
    }

    private static String config(String envName, String propertyName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(propertyName);
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private static String read(InputStream input) throws Exception {
        if (input == null) {
            return "";
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String match(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static final class TokenResponse {
        private final String accessToken;
        private final long expiresInSeconds;

        private TokenResponse(String accessToken, long expiresInSeconds) {
            this.accessToken = accessToken;
            this.expiresInSeconds = expiresInSeconds;
        }
    }
}
