package com.jbm.test.center;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MenuTest {


    @Test
    public void testMenuToExcel() {
        List<BaseMenu> list = new ArrayList<>();
        String json = ResourceUtil.readUtf8Str("classpath:menu.json");
        list = JSON.parseArray(json, BaseMenu.class);
        String fileName = "menus.xlsx";
        ExcelUtil.getBigWriter(fileName).write(list);
    }

}
