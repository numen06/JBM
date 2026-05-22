package com.jbm.cluster.core.security;

import cn.hutool.crypto.SecureUtil;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpMethod;

import java.security.KeyPair;
import java.util.Base64;

public class ApiSecurityUtilsTest {

    @Test
    public void signAndVerify_roundTrip() {
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 2048);
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        String content = ApiSecurityUtils.buildSignContent(
                HttpMethod.POST, "/api/test", "b=2&a=1", "{\"x\":1}",
                "1700000000000", "demo");

        String signature = ApiSecurityUtils.sign(content, privateKey);
        Assert.assertTrue(ApiSecurityUtils.verify(content, signature, publicKey));
    }

    @Test
    public void looksLikeRsaCiphertext_rejectsShortPlaintext() {
        Assert.assertFalse(ApiSecurityUtils.looksLikeRsaCiphertext("plain"));
    }

    @Test
    public void encryptDecrypt_roundTrip() {
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 2048);
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        String plain = "SecretP@ss1";
        String cipher = ApiSecurityUtils.encrypt(plain, publicKey);
        String decrypted = ApiSecurityUtils.decrypt(cipher, privateKey, publicKey);
        Assert.assertEquals(plain, decrypted);
    }
}
