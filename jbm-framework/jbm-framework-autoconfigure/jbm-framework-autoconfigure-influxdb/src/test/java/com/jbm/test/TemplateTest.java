package com.jbm.test;

import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.springframework.data.influx.BeetlSqlEngine;

public class TemplateTest {

    private TemplateEngine templateEngine;

    @Test
    public void test() {
        TemplateConfig templateConfig = new TemplateConfig("influx/sqls", TemplateConfig.ResourceMode.CLASSPATH);
        templateConfig.setCustomEngine(BeetlSqlEngine.class);
        templateEngine = TemplateUtil.createEngine(templateConfig);
        Template template = templateEngine.getTemplate("test" + ".sql");
        System.out.println(template.render(Maps.newConcurrentMap()));
    }
}
