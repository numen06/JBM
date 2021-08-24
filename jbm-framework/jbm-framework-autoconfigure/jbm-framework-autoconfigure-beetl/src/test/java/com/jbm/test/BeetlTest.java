package com.jbm.test;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import jbm.framework.boot.autoconfigure.beetl.BeetlAutoConfiguration;
import jbm.framework.boot.autoconfigure.beetl.BeetlProperties;
import jbm.framework.boot.autoconfigure.beetl.util.BeetlUtil;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//@RunWith(SpringRunner.class)
// @Import(BeetlProperties.class)
//@SpringBootTest(classes = BeetlAutoConfiguration.class)
public class BeetlTest {


    @Test
    public void exampleTest() throws IOException {
        Template temp = BeetlUtil.getClassPathTemplate("/", "test.txt");
        temp.binding("name", "wesley");
        System.out.println(temp.render());
    }

    @Test
    public void exampleTest2() throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", "wesley2");
        String str = BeetlUtil.render("root", "test.txt", map);
        System.out.println(str);
    }

    @Test
    public void exampleTest3() throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", "wesley3");
        GroupTemplate classpathload = BeetlUtil.createClassPathGroupTemplate(ClassUtil.getPackagePath(BeetlProperties.class));
        //只要target下有文件即可
        String str = BeetlUtil.render(classpathload.getTemplate("test.txt"), map);
        System.out.println(str);
    }

//    @Test
//    public void exampleTest3() throws IOException {
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put("name", "wesley3");
//        File file = beetlTemplate.render("/root/test.btl", map, FileUtil.createTempFile());
//        System.out.println(FileUtils.readFileToString(file));
//    }
//
//    @Test
//    public void exampleTest4() throws IOException {
//        Map<String, Object> map = new HashMap<String, Object>();
//        File file = new File("target/test");
//        FileUtils.forceMkdir(file);
//        file = beetlTemplate.render("test1.xml", map, new File("target/test/test1.xls"));
//        System.out.println(FileUtils.readFileToString(file));
//    }
}
