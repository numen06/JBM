package com.jbm.cluster.auth.service;

import cn.hutool.core.exceptions.ValidateException;
import com.jbm.cluster.common.basic.service.SysDebugModeService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaptchaSingleUseTest {

    @Test
    void imageCaptchaShouldOnlyVerifyOnce() {
        OneTimeCodeService oneTimeCodeService = mock(OneTimeCodeService.class);
        when(oneTimeCodeService.consume("/vcode/system/abc12"))
                .thenReturn("AbC12", null);
        VCoderService service = new VCoderService();
        ReflectionTestUtils.setField(service, "oneTimeCodeService", oneTimeCodeService);
        ReflectionTestUtils.setField(service, "sysDebugModeService", mock(SysDebugModeService.class));

        assertTrue(service.verify("AbC12"));
        assertThrows(ValidateException.class, () -> service.verify("AbC12"));
    }

    @Test
    void smsCaptchaShouldOnlyVerifyOnce() {
        OneTimeCodeService oneTimeCodeService = mock(OneTimeCodeService.class);
        when(oneTimeCodeService.consume("/vcode/13800138000"))
                .thenReturn("123456", null);
        PCoderService service = new PCoderService();
        ReflectionTestUtils.setField(service, "oneTimeCodeService", oneTimeCodeService);
        ReflectionTestUtils.setField(service, "sysDebugModeService", mock(SysDebugModeService.class));

        assertTrue(service.verify("123456", "13800138000"));
        assertThrows(ValidateException.class, () -> service.verify("123456", "13800138000"));
    }
}
