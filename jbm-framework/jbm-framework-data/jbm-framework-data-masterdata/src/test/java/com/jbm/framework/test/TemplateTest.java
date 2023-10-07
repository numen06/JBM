package com.jbm.framework.test;

import cn.hutool.core.map.MapUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import com.jbm.util.template.simple.SimpleTemplateEngine;
import org.junit.jupiter.api.Test;

public class TemplateTest {

    private final static String MASTERDATA_TEMP_PATH = "com/jbm/framework/masterdata/code/btl/";

    @Test
    public void testTemplate() {
        TemplateConfig templateConfig = new TemplateConfig(MASTERDATA_TEMP_PATH, TemplateConfig.ResourceMode.CLASSPATH);
        templateConfig = templateConfig.setCustomEngine(SimpleTemplateEngine.class);
        TemplateEngine templateEngine = TemplateUtil.createEngine(templateConfig);

        Template template = templateEngine.getTemplate("business.btl");

        System.out.println(template.render(MapUtil.of("fileName","test")));
    }
}


