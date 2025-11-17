package com.jbm.cluster.push;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.mail.MailUtil;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-03-06 04:21
 **/
public class MailTest {

    @Test
    public void testMail() {
        MailUtil.send("jason.peng@feg.cn", "测试", "邮件来自Hutool测试" + DateUtil.now(), false);
    }

    @Test
    public void between() {
        System.out.println(DateUtil.between(DateUtil.offsetMinute(new Date(), 1), DateTime.now(), DateUnit.MINUTE));
    }

}
