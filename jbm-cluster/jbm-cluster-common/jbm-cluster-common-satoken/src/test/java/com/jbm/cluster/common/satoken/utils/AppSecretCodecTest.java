package com.jbm.cluster.common.satoken.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppSecretCodecTest {

    @Test
    void encryptDecryptRoundTrip() {
        String plain = "jbmSeedDevSecret0000000001";
        String stored = AppSecretCodec.encrypt(plain);
        assertThat(stored).startsWith(AppSecretCodec.ENC_PREFIX);
        assertThat(AppSecretCodec.decrypt(stored)).isEqualTo(plain);
        assertThat(AppSecretCodec.verify(plain, stored)).isTrue();
    }

    @Test
    void verifySupportsLegacyBcrypt() {
        String plain = "test-secret-123";
        String bcrypt = SecurityUtils.encryptPassword(plain);
        assertThat(AppSecretCodec.decrypt(bcrypt)).isNull();
        assertThat(AppSecretCodec.verify(plain, bcrypt)).isTrue();
        assertThat(AppSecretCodec.verify("wrong", bcrypt)).isFalse();
    }
}
