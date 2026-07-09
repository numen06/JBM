package com.jbm.cluster.common.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InnerAuthContextTest {

    @AfterEach
    void tearDown() {
        InnerAuthContext.clear();
    }

    @Test
    void shouldSkipPermissionCheck_whenInnerClientTokenValidated() {
        InnerAuthContext.setValidated(true);
        InnerAuthContext.setSkipPermissionCheck(true);

        assertTrue(InnerAuthContext.isValidated());
        assertTrue(InnerAuthContext.shouldSkipPermissionCheck());
    }

    @Test
    void shouldNotSkipPermissionCheck_byDefault() {
        assertFalse(InnerAuthContext.isValidated());
        assertFalse(InnerAuthContext.shouldSkipPermissionCheck());
    }

    @Test
    void clear_removesThreadLocalState() {
        InnerAuthContext.setValidated(true);
        InnerAuthContext.setSkipPermissionCheck(true);

        InnerAuthContext.clear();

        assertFalse(InnerAuthContext.isValidated());
        assertFalse(InnerAuthContext.shouldSkipPermissionCheck());
    }
}
