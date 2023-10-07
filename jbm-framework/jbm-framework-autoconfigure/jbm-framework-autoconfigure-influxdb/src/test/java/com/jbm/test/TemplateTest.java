package com.jbm.test;

import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import com.google.common.collect.Maps;
import com.jbm.util.template.simple.SimpleTemplateEngine;
import org.junit.jupiter.api.Test;

public class TemplateTest {

    private TemplateEngine templateEngine;

    @Test
    public void test() {
        TemplateConfig templateConfig = new TemplateConfig("influx/sqls", TemplateConfig.ResourceMode.CLASSPATH);
        templateConfig.setCustomEngine(SimpleTemplateEngine.class);
        templateEngine = TemplateUtil.createEngine(templateConfig);
        Template template = templateEngine.getTemplate("test" + ".sql");
        System.out.println(template.render(Maps.newConcurrentMap()));
    }
}
