package com.jbm.cluster.common.satoken.standardjwt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.utils.LoginHelper;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issues standard RS256 OAuth2 Bearer JWTs while Sa-Token keeps the OAuth2 flow and annotations.
 */
public class StandardJwtIssuer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StandardJwtIssuerProperties properties;
    private volatile PrivateKey privateKey;

    public StandardJwtIssuer(StandardJwtIssuerProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return StrUtil.isNotBlank(properties.getPrivateKey())
                && StrUtil.isNotBlank(properties.getIssuer())
                && StrUtil.isNotBlank(properties.getAudience());
    }

    public String issueUserToken(String clientId, Object loginId, String scope) {
        if (!isEnabled()) {
            return null;
        }
        JbmLoginUser loginUser = LoginHelper.softGetLoginUser();
        if (loginUser == null && loginId != null) {
            try {
                loginUser = LoginHelper.getLoginUser(loginId);
            } catch (Exception ignored) {
            }
        }
        Map<String, Object> claims = baseClaims(String.valueOf(loginId), clientId, scope,
                properties.getAccessTokenTtlSeconds());
        if (loginUser != null) {
            putIfNotNull(claims, "user_id", loginUser.getUserId());
            putIfNotBlank(claims, "username", loginUser.getUsername());
            putIfNotBlank(claims, "user_type", loginUser.getUserType());
            putIfNotNull(claims, "app_id", loginUser.getAppId());
            putIfNotNull(claims, "tenant_id", loginUser.getCompanyId());
            if (CollUtil.isNotEmpty(loginUser.getMenuPermission())) {
                claims.put("permissions", new ArrayList<String>(loginUser.getMenuPermission()));
            }
            if (CollUtil.isNotEmpty(loginUser.getRoles())) {
                claims.put("roles", new ArrayList<String>(loginUser.getRoles()));
            }
        }
        return sign(claims);
    }

    public String issueClientToken(String clientId, String scope) {
        if (!isEnabled()) {
            return null;
        }
        Map<String, Object> claims = baseClaims("client:" + clientId, clientId, scope,
                properties.getClientTokenTtlSeconds());
        claims.put("user_type", "client");
        return sign(claims);
    }

    public Map<String, Object> jwks() {
        Map<String, Object> jwks = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> keys = new ArrayList<Map<String, Object>>();
        if (isEnabled()) {
            try {
                RSAPrivateCrtKey key = (RSAPrivateCrtKey) getPrivateKey();
                Map<String, Object> jwk = new LinkedHashMap<String, Object>();
                jwk.put("kty", "RSA");
                jwk.put("use", "sig");
                jwk.put("kid", properties.getKeyId());
                jwk.put("alg", "RS256");
                jwk.put("n", base64UrlUnsigned(key.getModulus()));
                jwk.put("e", base64UrlUnsigned(key.getPublicExponent()));
                keys.add(jwk);
            } catch (Exception ignored) {
            }
        }
        jwks.put("keys", keys);
        return jwks;
    }

    private Map<String, Object> baseClaims(String subject, String clientId, String scope, long ttlSeconds) {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("iss", properties.getIssuer());
        claims.put("aud", properties.getAudience());
        claims.put("sub", subject);
        claims.put("client_id", clientId);
        claims.put("scope", StrUtil.blankToDefault(scope, ""));
        claims.put("iat", now);
        claims.put("nbf", now);
        claims.put("exp", now + Math.max(300L, ttlSeconds));
        claims.put("jti", UUID.randomUUID().toString());
        List<String> scopes = splitScope(scope);
        if (!scopes.isEmpty()) {
            claims.put("scopes", scopes);
        }
        return claims;
    }

    private String sign(Map<String, Object> claims) {
        try {
            Map<String, Object> header = new LinkedHashMap<String, Object>();
            header.put("alg", "RS256");
            header.put("typ", "JWT");
            header.put("kid", properties.getKeyId());
            String encodedHeader = base64Url(OBJECT_MAPPER.writeValueAsBytes(header));
            String encodedClaims = base64Url(OBJECT_MAPPER.writeValueAsBytes(claims));
            byte[] signingInput = (encodedHeader + "." + encodedClaims).getBytes(StandardCharsets.US_ASCII);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(getPrivateKey());
            signature.update(signingInput);
            return encodedHeader + "." + encodedClaims + "." + base64Url(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue standard OAuth2 JWT", e);
        }
    }

    private PrivateKey getPrivateKey() throws Exception {
        if (privateKey != null) {
            return privateKey;
        }
        synchronized (this) {
            if (privateKey == null) {
                String normalized = properties.getPrivateKey()
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                byte[] encoded = Base64.getDecoder().decode(normalized);
                privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
            }
            return privateKey;
        }
    }

    private static List<String> splitScope(String scope) {
        List<String> scopes = new ArrayList<String>();
        if (StrUtil.isBlank(scope)) {
            return scopes;
        }
        for (String item : StrUtil.split(scope, ' ')) {
            if (StrUtil.isNotBlank(item)) {
                scopes.add(item.trim());
            }
        }
        return scopes;
    }

    private static void putIfNotNull(Map<String, Object> claims, String key, Object value) {
        if (value != null) {
            claims.put(key, value);
        }
    }

    private static void putIfNotBlank(Map<String, Object> claims, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            claims.put(key, value);
        }
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String base64UrlUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return base64Url(bytes);
    }
}
