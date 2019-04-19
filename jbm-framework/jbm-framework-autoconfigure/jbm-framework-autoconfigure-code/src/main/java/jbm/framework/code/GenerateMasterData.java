package jbm.framework.code;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * @author: create by wesley
 * @date:2019/4/18
 */
public class GenerateMasterData {


    private GroupTemplate gt;
    private String basePackage;
    private Class<?> entityClass;
    private Boolean skip;

    public void setSkip(Boolean skip) {
        this.skip = skip;
    }

    public GenerateMasterData() {
        try {
            ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("/jbm/framework/code/btl");
            Configuration cfg = Configuration.defaultConfiguration();
            gt = new GroupTemplate(resourceLoader, cfg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public GenerateMasterData(Class<?> entityClass) {
        this();
        this.entityClass = entityClass;
        this.basePackage = ClassUtil.getPackage(entityClass);
    }

    public GenerateMasterData(Class<?> entityClass, String basePackage) {
        this(entityClass);
        this.basePackage = basePackage;
    }

    //首字母转小写
    private static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
    }

    //首字母转大写
    public static String toUpperCaseFirstOne(String s) {
        if (Character.isUpperCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
    }

    private Template buildData(CodeType codeType) {
        Template t = gt.getTemplate(codeType.name() + ".btl");
        t.binding("entityClass", entityClass);
        t.binding("mapping", toLowerCaseFirstOne(entityClass.getSimpleName()));
        t.binding("basePackage", basePackage);
        t.binding("time", DateUtil.now());
        return t;
    }


    private File getWriteFile(CodeType codeType) {
        URL url = ClassUtil.getResourceUrl("/", this.entityClass);
        try {
            String temp = basePackage.replace(".", "/");
            Path wp = Paths.get(url.toURI()).getParent().getParent().resolve("src").resolve("main").resolve("java").resolve(temp);
            switch (codeType) {
                case serviceImpl:
                    wp = wp.resolve(CodeType.service.name()).resolve("impl");
                    break;
                default:
                    wp = wp.resolve(codeType.name());
                    break;
            }
            FileUtil.mkdir(wp.toFile());
            String fileName = this.entityClass.getSimpleName() + toUpperCaseFirstOne(codeType.name()) + ".java";
            File file = wp.resolve(fileName).toFile();
            if (FileUtil.exist(file) && this.skip) {
                return null;
            }
            System.out.println(file);
            return file;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void generate(CodeType codeType) {
        Template t = buildData(codeType);
        File file = this.getWriteFile(codeType);
        if (file == null)
            return;
        t.renderTo(FileUtil.getOutputStream(file));
    }



    public void generateAll() {
        generate(CodeType.mapper);
        generate(CodeType.service);
        generate(CodeType.serviceImpl);
        generate(CodeType.controller);
    }

    public static void scanGnerate(Class<?> entityClass) {
        Set<Class<?>> entitys = ClassUtil.scanPackage(ClassUtil.getPackage(entityClass));
        for (Class clazz : entitys) {
            GenerateMasterData generateMasterData = new GenerateMasterData(clazz);
            generateMasterData.setSkip(true);
            generateMasterData.generateAll();
        }
    }

    public static void scanGnerate(Class<?> entityClass, String basePackage) {
        Set<Class<?>> entitys = ClassUtil.scanPackage(ClassUtil.getPackage(entityClass));
        for (Class clazz : entitys) {
            GenerateMasterData generateMasterData = new GenerateMasterData(clazz, basePackage);
            generateMasterData.setSkip(true);
            generateMasterData.generateAll();
        }
    }



}
