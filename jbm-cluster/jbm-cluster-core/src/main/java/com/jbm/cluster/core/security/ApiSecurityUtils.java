package com.jbm.cluster.core.security;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestUtil;
import com.jbm.cluster.core.constant.ApiSecurityConstants;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * RSA 签名 / 加解密工具（Gateway 验签、Auth 登录、客户端 SDK 共用）
 */
public final class ApiSecurityUtils {

    private static final int RSA_CIPHER_MIN_LENGTH = 64;

    private ApiSecurityUtils() {
    }

    public static String buildSignContent(HttpMethod method, String path, String query,
                                          String body, String timestamp, String appId) {
        StringBuilder sb = new StringBuilder();
        sb.append(method != null ? method.name() : "").append('\n');
        sb.append(StrUtil.nullToEmpty(path)).append('\n');
        sb.append(StrUtil.isNotBlank(query) ? sortQueryString(query) : "").append('\n');
        if (StrUtil.isNotBlank(body)) {
            sb.append(Base64.encode(DigestUtil.md5(body)));
        }
        sb.append('\n');
        sb.append(StrUtil.nullToEmpty(timestamp)).append('\n');
        sb.append(StrUtil.nullToEmpty(appId));
        return sb.toString();
    }

    public static String buildSignContent(ServerHttpRequest request, String body,
                                          String timestamp, String appId) {
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        return buildSignContent(request.getMethod(), path, query, body, timestamp, appId);
    }

    public static String sign(String content, String privateKeyBase64) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            RSA rsa = SecureUtil.rsa(privateKeyBase64, null);
            signature.initSign(rsa.getPrivateKey());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.encode(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("RSA sign failed", e);
        }
    }

    public static boolean verify(String content, String signatureBase64, String publicKeyBase64) {
        if (StrUtil.hasBlank(content, signatureBase64, publicKeyBase64)) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            RSA rsa = SecureUtil.rsa(null, publicKeyBase64);
            signature.initVerify(rsa.getPublicKey());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    public static String encrypt(String plainText, String publicKeyBase64) {
        RSA rsa = SecureUtil.rsa(null, publicKeyBase64);
        return rsa.encryptBase64(plainText, KeyType.PublicKey);
    }

    public static String decrypt(String cipherText, String privateKeyBase64, String publicKeyBase64) {
        RSA rsa = SecureUtil.rsa(privateKeyBase64, publicKeyBase64);
        return rsa.decryptStr(cipherText, KeyType.PrivateKey);
    }

    public static boolean looksLikeRsaCiphertext(String value) {
        if (StrUtil.isBlank(value) || value.length() < RSA_CIPHER_MIN_LENGTH) {
            return false;
        }
        try {
            Base64.decode(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPasswordEncryptedHeader(String headerValue) {
        return ApiSecurityConstants.PASSWORD_ENCRYPTED_VALUE
                .equalsIgnoreCase(StrUtil.trim(headerValue));
    }

    public static String sortQueryString(String query) {
        if (StrUtil.isBlank(query)) {
            return "";
        }
        List<String> pairs = new ArrayList<>();
        for (String part : query.split("&")) {
            if (StrUtil.isNotBlank(part)) {
                pairs.add(part);
            }
        }
        Collections.sort(pairs);
        return pairs.stream().collect(Collectors.joining("&"));
    }

    public static boolean isTimestampValid(String timestampMs, long expireMs) {
        if (StrUtil.isBlank(timestampMs)) {
            return false;
        }
        try {
            long ts = Long.parseLong(timestampMs.trim());
            return Math.abs(System.currentTimeMillis() - ts) <= expireMs;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
