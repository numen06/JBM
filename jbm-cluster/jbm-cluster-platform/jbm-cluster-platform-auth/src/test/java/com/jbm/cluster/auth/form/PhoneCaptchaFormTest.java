package com.jbm.cluster.auth.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PhoneCaptchaFormTest {

    @Test
    public void resolveSmsCodePrefersPcode() {
        PhoneCaptchaForm form = new PhoneCaptchaForm();
        form.setPcode("123456");
        form.setVcode("9999");
        assertEquals("123456", form.resolveSmsCode());
    }

    @Test
    public void resolveSmsCodeFallsBackToVcode() {
        PhoneCaptchaForm form = new PhoneCaptchaForm();
        form.setVcode("654321");
        assertEquals("654321", form.resolveSmsCode());
    }

    @Test
    public void resolveSmsCodeTrimsWhitespace() {
        PhoneCaptchaForm form = new PhoneCaptchaForm();
        form.setPcode(" 123456 ");
        assertEquals("123456", form.resolveSmsCode());
    }

    @Test
    public void resolveSmsCodeReturnsNullWhenBlank() {
        PhoneCaptchaForm form = new PhoneCaptchaForm();
        assertNull(form.resolveSmsCode());
    }
}
