package com.jbm.cluster.platform.auth.test;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.junit.Test;

import java.io.File;

public class CaptchaTest {

    @Test
    public void testCreate() {
        //定义图形验证码的长和宽
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200, 100);
        //LineCaptcha lineCaptcha = new LineCaptcha(200, 100, 4, 150);
        //图形验证码写出，可以写出到文件，也可以写出到流
        lineCaptcha.write(new File("line.png"));
        //验证图形验证码的有效性，返回boolean值
        lineCaptcha.verify("1234");
    }
}
