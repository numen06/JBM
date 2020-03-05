package com.jbm.cluster.push;

import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.mail.MailUtil;
import org.junit.Test;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-06 04:21
 **/
public class MailTest {

    @Test
    public void testMail() {
        MailUtil.send("numen06@qq.com", "测试", "邮件来自Hutool测试" + DateUtil.now(), false);
    }
}
