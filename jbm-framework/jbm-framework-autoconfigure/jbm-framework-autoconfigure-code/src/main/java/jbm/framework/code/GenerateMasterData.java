package jbm.framework.code;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final static Logger logger = LoggerFactory.getLogger(GenerateMasterData.class);


    private GroupTemplate gt;
    private String basePackage;
    private Class<?> entityClass;
    private Boolean skip;
    private Boolean ignore;

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
        this.ignore = this.entityClass.getAnnotationsByType(IgnoreGeneate.class).length > 0;
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
//        if (!ArrayUtil.contains(this.entityClass.getInterfaces(), ClassUtil.loadClass("com.jbm.framework.masterdata.usage.bean.MasterDataEntity"))) {
//            return;
//        }
        if (this.ignore) {
            return;
        }
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
        String entityPackage = ClassUtil.getPackage(entityClass);
        String targetPackage = ClassUtil.getPackage(entityClass);
        scanGnerate(entityPackage, targetPackage);
    }

    public static void scanGnerate(Class<?> entityClass, String targetPackage) {
        String entityPackage = ClassUtil.getPackage(entityClass);
        scanGnerate(entityPackage, targetPackage);
    }

    public static void scanGnerate(String entityPackage, String targetPackage) {
        try {
            logger.info("自动扫描生成代码:{},目标包名:{}", entityPackage, targetPackage);
            Set<Class<?>> entitys = ClassUtil.scanPackage(entityPackage);
            for (Class clazz : entitys) {
                try {
                    GenerateMasterData generateMasterData = new GenerateMasterData(clazz, targetPackage);
                    generateMasterData.setSkip(true);
                    generateMasterData.generateAll();
                } catch (Exception e) {
                    logger.error("生成代码错误Class:{}", clazz, e);
                }
            }
        } catch (Exception e) {
            logger.error("生成代码错误", e);
        }
    }


}
