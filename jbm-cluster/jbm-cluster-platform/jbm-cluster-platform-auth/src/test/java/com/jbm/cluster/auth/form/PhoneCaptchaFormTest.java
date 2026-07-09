package com.jbm.cluster.auth.form;

import org.junit.Assert;
import org.junit.Test;

public class PhoneCaptchaFormTest {

    @Test
    public void resolveSmsCodePrefersPcode() {
        PhoneCaptchaForm form = new PhoneCaptchaForm();
        form.setPcode("123456");
        form.setVcode("9999");
        Assert.assertEquals("123456", form.resolveSmsCode());
    }

    @Test
    public void resolveSmsCodeFallsBackToVcode() {
        PhoneCaptchaForm form = new PhoneCaptchaForm();
        form.setVcode("654321");
        Assert.assertEquals("654321", form.resolveSmsCode());
    }

    @Test
    public void resolveSmsCodeTrimsWhitespace() {
        PhoneCaptchaForm form = new PhoneCaptchaForm();
        form.setPcode(" 123456 ");
        Assert.assertEquals("123456", form.resolveSmsCode());
    }

    @Test
    public void resolveSmsCodeReturnsNullWhenBlank() {
        PhoneCaptchaForm form = new PhoneCaptchaForm();
        Assert.assertNull(form.resolveSmsCode());
    }
}
