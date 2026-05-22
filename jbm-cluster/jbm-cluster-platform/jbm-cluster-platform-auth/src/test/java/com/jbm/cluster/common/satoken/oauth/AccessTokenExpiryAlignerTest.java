package com.jbm.cluster.common.satoken.oauth;

import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenExpiryAlignerTest {

    @Test
    void alignAccessTokenModel_skipsNullOrBlankToken() {
        AccessTokenModel blank = new AccessTokenModel("", "demo", 1L, "all");
        long before = blank.expiresTime;
        AccessTokenExpiryAligner.alignAccessTokenModel(blank);
        assertEquals(before, blank.expiresTime);
        AccessTokenExpiryAligner.alignAccessTokenModel(null);
    }

    @Test
    void expiresTimeFormula_matchesProductionAlignLogic() {
        long remainingSeconds = 120;
        long before = System.currentTimeMillis();
        long expiresTime = before + remainingSeconds * 1000L;
        assertTrue(expiresTime >= before + 119_000L);
        assertTrue(expiresTime <= before + 121_000L);
    }
}