package com.jbm.test;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.github.pfmiles.minvelocity.TemplateUtil;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class BannerTest {

    @Test
    public void testBanner() throws IOException {
        // using default font standard.flf, obtained from maven artifact
        Map<String, String> bal = Maps.newHashMap();
        bal.put("test", DateUtil.now());
        String asciiArt1 = TemplateUtil.render("test.banner", bal);
        System.out.println(asciiArt1);

    }

    @Test
    public void testBanner2() {
        // using default font standard.flf, obtained from maven artifact
        InputStream inputStream = ResourceUtil.getStream("test.banner");
        System.out.println(IoUtil.readUtf8(inputStream));

    }


}
