package com.jbm.cluster.common.satoken.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

/**
 * 应用 Client Secret 编解码：新写入使用 AES 可逆存储，兼容历史 BCrypt / 明文。
 */
public final class AppSecretCodec {

    public static final String ENC_PREFIX = "$ENC$";

    private static final byte[] AES_KEY = SecureUtil.md5("jbm-app-client-secret").getBytes(StandardCharsets.UTF_8);

    private AppSecretCodec() {
    }

    public static String encrypt(String plainSecret) {
        if (StrUtil.isBlank(plainSecret)) {
            return plainSecret;
        }
        AES aes = SecureUtil.aes(AES_KEY);
        return ENC_PREFIX + aes.encryptBase64(plainSecret);
    }

    public static String decrypt(String storedSecret) {
        if (StrUtil.isBlank(storedSecret)) {
            return null;
        }
        if (storedSecret.startsWith(ENC_PREFIX)) {
            AES aes = SecureUtil.aes(AES_KEY);
            return aes.decryptStr(storedSecret.substring(ENC_PREFIX.length()));
        }
        if (storedSecret.startsWith("$2a$") || storedSecret.startsWith("$2b$")) {
            return null;
        }
        return storedSecret;
    }

    public static boolean verify(String plainSecret, String storedSecret) {
        if (StrUtil.hasBlank(plainSecret, storedSecret)) {
            return false;
        }
        if (storedSecret.startsWith(ENC_PREFIX)) {
            return plainSecret.equals(decrypt(storedSecret));
        }
        if (storedSecret.startsWith("$2a$") || storedSecret.startsWith("$2b$")) {
            return SecurityUtils.matchesPassword(plainSecret, storedSecret);
        }
        return plainSecret.equals(storedSecret);
    }
}
