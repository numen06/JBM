package com.jbm.cluster.common.satoken.standardjwt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class StandardJwtVerifier {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final StandardJwtProperties properties;
    private final StandardJwtLoginConverter loginConverter;
    private volatile List<Map<String, Object>> jwksKeys = Collections.emptyList();
    private volatile long jwksLoadedAtMillis = 0L;
    private volatile PublicKey configuredPublicKey;

    public StandardJwtVerifier(StandardJwtProperties properties, StandardJwtLoginConverter loginConverter) {
        this.properties = properties;
        this.loginConverter = loginConverter;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public StandardJwtPrincipal verify(String token) {
        if (!properties.isEnabled() || StrUtil.isBlank(token)) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            Map<String, Object> header = readJson(parts[0]);
            Map<String, Object> claims = readJson(parts[1]);
            String alg = String.valueOf(header.get("alg"));
            if (!isAcceptedAlgorithm(alg)) {
                log.debug("[标准JWT] 拒绝不支持的算法: {}", alg);
                return null;
            }
            byte[] signingInput = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
            byte[] signature = base64UrlDecode(parts[2]);
            if (!verifySignature(alg, header, signingInput, signature)) {
                log.debug("[标准JWT] 签名校验失败");
                return null;
            }
            if (!validateClaims(claims)) {
                return null;
            }
            String loginId = loginConverter.resolveLoginId(claims);
            if (StrUtil.isBlank(loginId)) {
                log.debug("[标准JWT] 无法从 claims 解析 loginId");
                return null;
            }
            return new StandardJwtPrincipal(token, loginId, claims, loginConverter.toLoginUser(token, claims));
        } catch (Exception e) {
            log.debug("[标准JWT] 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private boolean validateClaims(Map<String, Object> claims) {
        long now = System.currentTimeMillis() / 1000L;
        long skew = Math.max(0L, properties.getClockSkewSeconds());
        Long exp = numberClaim(claims.get("exp"));
        if (exp != null && now - skew > exp) {
            log.debug("[标准JWT] token 已过期");
            return false;
        }
        Long nbf = numberClaim(claims.get("nbf"));
        if (nbf != null && now + skew < nbf) {
            log.debug("[标准JWT] token 尚未生效");
            return false;
        }
        if (StrUtil.isNotBlank(properties.getIssuer())) {
            String iss = stringClaim(claims.get("iss"));
            if (!properties.getIssuer().equals(iss)) {
                log.debug("[标准JWT] issuer 不匹配: {}", iss);
                return false;
            }
        }
        if (StrUtil.isNotBlank(properties.getAudience())) {
            if (!matchesAudience(claims.get("aud"), properties.getAudience())) {
                log.debug("[标准JWT] audience 不匹配");
                return false;
            }
        }
        return true;
    }

    private boolean verifySignature(String alg, Map<String, Object> header, byte[] signingInput, byte[] signature) throws Exception {
        if (alg.startsWith("RS")) {
            PublicKey key = resolvePublicKey(header);
            if (key == null) {
                return false;
            }
            Signature verifier = Signature.getInstance(jcaRsaAlgorithm(alg));
            verifier.initVerify(key);
            verifier.update(signingInput);
            return verifier.verify(signature);
        }
        if (alg.startsWith("HS")) {
            if (StrUtil.isBlank(properties.getSharedSecret())) {
                return false;
            }
            Mac mac = Mac.getInstance(jcaHmacAlgorithm(alg));
            mac.init(new SecretKeySpec(properties.getSharedSecret().getBytes(StandardCharsets.UTF_8), mac.getAlgorithm()));
            byte[] expected = mac.doFinal(signingInput);
            return java.security.MessageDigest.isEqual(expected, signature);
        }
        return false;
    }

    private PublicKey resolvePublicKey(Map<String, Object> header) throws Exception {
        String kid = stringClaim(header.get("kid"));
        if (StrUtil.isNotBlank(properties.getJwksUri())) {
            for (Map<String, Object> jwk : loadJwks()) {
                String currentKid = stringClaim(jwk.get("kid"));
                if (StrUtil.isBlank(kid) || StrUtil.equals(kid, currentKid)) {
                    PublicKey key = publicKeyFromJwk(jwk);
                    if (key != null) {
                        return key;
                    }
                }
            }
        }
        if (configuredPublicKey != null) {
            return configuredPublicKey;
        }
        if (StrUtil.isBlank(properties.getPublicKey())) {
            return null;
        }
        configuredPublicKey = publicKeyFromPem(properties.getPublicKey());
        return configuredPublicKey;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadJwks() throws Exception {
        long now = System.currentTimeMillis();
        long cacheMillis = Math.max(1L, properties.getJwksCacheSeconds()) * 1000L;
        if (CollUtil.isNotEmpty(jwksKeys) && now - jwksLoadedAtMillis < cacheMillis) {
            return jwksKeys;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (CollUtil.isNotEmpty(jwksKeys) && now - jwksLoadedAtMillis < cacheMillis) {
                return jwksKeys;
            }
            byte[] data = readAll(new URL(properties.getJwksUri()).openStream());
            Map<String, Object> jwks = OBJECT_MAPPER.readValue(data, MAP_TYPE);
            Object keys = jwks.get("keys");
            List<Map<String, Object>> parsed = new ArrayList<Map<String, Object>>();
            if (keys instanceof Collection) {
                for (Object key : (Collection<?>) keys) {
                    if (key instanceof Map) {
                        parsed.add((Map<String, Object>) key);
                    }
                }
            }
            jwksKeys = parsed;
            jwksLoadedAtMillis = now;
            return jwksKeys;
        }
    }

    private static PublicKey publicKeyFromJwk(Map<String, Object> jwk) throws Exception {
        if (!"RSA".equalsIgnoreCase(stringClaim(jwk.get("kty")))) {
            return null;
        }
        String n = stringClaim(jwk.get("n"));
        String e = stringClaim(jwk.get("e"));
        if (StrUtil.hasBlank(n, e)) {
            return null;
        }
        BigInteger modulus = new BigInteger(1, base64UrlDecode(n));
        BigInteger exponent = new BigInteger(1, base64UrlDecode(e));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private static RSAPublicKey publicKeyFromPem(String pem) throws Exception {
        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(normalized);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static String jcaRsaAlgorithm(String alg) {
        if ("RS384".equals(alg)) {
            return "SHA384withRSA";
        }
        if ("RS512".equals(alg)) {
            return "SHA512withRSA";
        }
        return "SHA256withRSA";
    }

    private static String jcaHmacAlgorithm(String alg) {
        if ("HS384".equals(alg)) {
            return "HmacSHA384";
        }
        if ("HS512".equals(alg)) {
            return "HmacSHA512";
        }
        return "HmacSHA256";
    }

    private boolean isAcceptedAlgorithm(String alg) {
        if (StrUtil.isBlank(alg) || "none".equalsIgnoreCase(alg)) {
            return false;
        }
        List<String> accepted = properties.getAcceptedAlgorithms();
        if (accepted == null || accepted.isEmpty()) {
            return "RS256".equals(alg) || "RS384".equals(alg) || "RS512".equals(alg)
                    || "HS256".equals(alg) || "HS384".equals(alg) || "HS512".equals(alg);
        }
        return accepted.contains(alg);
    }

    private static boolean matchesAudience(Object audClaim, String expected) {
        if (audClaim instanceof Collection) {
            for (Object item : (Collection<?>) audClaim) {
                if (expected.equals(stringClaim(item))) {
                    return true;
                }
            }
            return false;
        }
        return expected.equals(stringClaim(audClaim));
    }

    private static Long numberClaim(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String stringClaim(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Map<String, Object> readJson(String base64Url) throws Exception {
        return OBJECT_MAPPER.readValue(base64UrlDecode(base64Url), MAP_TYPE);
    }

    private static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private static byte[] readAll(InputStream inputStream) throws Exception {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            inputStream.close();
        }
    }
}
