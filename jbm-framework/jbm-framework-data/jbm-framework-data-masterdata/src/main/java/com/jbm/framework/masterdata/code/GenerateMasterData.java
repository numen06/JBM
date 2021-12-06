package com.jbm.framework.masterdata.code;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.service.IMultiPlatformService;
import com.jbm.framework.masterdata.usage.entity.*;
import com.jbm.util.AnnotatedUtils;
import com.jbm.util.StringUtils;
import io.swagger.annotations.ApiModel;
import jodd.util.StringUtil;
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
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static com.jbm.framework.masterdata.code.CodeType.*;


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
    private Class superclass;


    public void setSkip(Boolean skip) {
        this.skip = skip;
    }

    public GenerateMasterData() {
        try {
            ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("/com/jbm/framework/masterdata/code/btl");
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
        if (ClassUtil.isAbstract(this.entityClass)) {
            this.ignore = true;
        }
        this.basePackage = ClassUtil.getPackage(entityClass);
        if (MasterDataEntity.class.isAssignableFrom(entityClass))
            this.superclass = entityClass.getSuperclass();
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

    private Template buildData(CodeType codeType) throws Exception {
        Template t = gt.getTemplate(codeType.name() + ".btl");
        t.binding("entityClass", entityClass);
        t.binding("mapping", toLowerCaseFirstOne(entityClass.getSimpleName()));
        t.binding("basePackage", basePackage);
        t.binding("time", DateUtil.now());

        try {
            //获取实体类上的注解
            Object value = AnnotationUtil.getAnnotationValue(entityClass, ApiModel.class);
            if (ObjectUtil.isNotEmpty(value)) {
                t.binding("entityDesc", value);
            } else {
                t.binding("entityDesc", entityClass.getSimpleName());
            }
        } catch (Exception e) {

        }

        String extClass = null;
        switch (codeType) {
            case mapper:
                extClass = SuperMapper.class.getName();
                break;
            case mapperXml:
                extClass = SuperMapper.class.getName();
                break;
            case service:
                if (superclass.equals(MasterDataEntity.class)) {
                    extClass = IMasterDataService.class.getName();
                }
                if (superclass.equals(MasterDataIdEntity.class)) {
                    extClass = IMasterDataService.class.getName();
                }
                if (superclass.equals(MasterDataCodeEntity.class)) {
                    extClass = IMasterDataService.class.getName();
                }
                if (superclass.equals(MasterDataTreeEntity.class)) {
                    extClass = IMasterDataTreeService.class.getName();
                }
                if (superclass.equals(MultiPlatformEntity.class)) {
                    extClass = IMultiPlatformService.class.getName();
                }
                if (superclass.equals(MultiPlatformIdEntity.class)) {
                    extClass = IMultiPlatformService.class.getName();
                }
                if (superclass.equals(MultiPlatformTreeEntity.class)) {
                    extClass = IMultiPlatformService.class.getName();
                }
                break;
            case serviceImpl:
                if (superclass.equals(MasterDataEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MasterDataServiceImpl";
                }
                if (superclass.equals(MasterDataIdEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MasterDataServiceImpl";
                }
                if (superclass.equals(MasterDataCodeEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MasterDataServiceImpl";
                }
                if (superclass.equals(MasterDataTreeEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MasterDataTreeServiceImpl";
                }
                if (superclass.equals(MultiPlatformEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MultiPlatformServiceImpl";
                }
                if (superclass.equals(MultiPlatformIdEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MultiPlatformServiceImpl";
                }
                if (superclass.equals(MultiPlatformTreeEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MultiPlatformTreeServiceImpl";
                }
                break;
            case controller:
                if (superclass.equals(MasterDataEntity.class)) {
                    extClass = "com.jbm.framework.mvc.web.MasterDataCollection";
                }
                if (superclass.equals(MasterDataIdEntity.class)) {
                    extClass = "com.jbm.framework.mvc.web.MasterDataCollection";
                }
                if (superclass.equals(MasterDataCodeEntity.class)) {
                    extClass = "com.jbm.framework.mvc.web.MasterDataCollection";
                }
                if (superclass.equals(MasterDataTreeEntity.class)) {
                    extClass = "com.jbm.framework.mvc.web.MasterDataTreeCollection";
                }
                if (superclass.equals(MultiPlatformEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MultiPlatformCollection";
                }
                if (superclass.equals(MultiPlatformIdEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MultiPlatformCollection";
                }
                if (superclass.equals(MultiPlatformTreeEntity.class)) {
                    extClass = "com.jbm.framework.service.mybatis.MultiPlatformTreeCollection";
                }
                break;
        }
        if (StringUtil.isBlank(extClass))
            throw new ClassNotFoundException("未发现匹配的父类" + superclass.getName());
        t.binding("extClass", extClass);
        t.binding("extClassName", StringUtils.substringAfterLast(extClass, "."));
        return t;
    }


    private File getWriteFile(CodeType codeType) {
        URL url = ClassUtil.getResourceUrl("/", this.entityClass);
        try {
            String temp = basePackage.replace(".", "/");
            Path wp = Paths.get(url.toURI()).getParent().getParent().resolve("src").resolve("main").resolve("java").resolve(temp);
            String ext = ".java";
            String suffix = toUpperCaseFirstOne(codeType.name());
            switch (codeType) {
                case mapperXml:
                    wp = Paths.get(url.toURI()).getParent().getParent().resolve("src").resolve("main").resolve("resources").resolve("mapper");
                    ext = ".xml";
                    suffix = "Mapper";
                    break;
                case serviceImpl:
                    wp = wp.resolve(service.name()).resolve("impl");
                    break;
                default:
                    wp = wp.resolve(codeType.name());
                    break;
            }
            FileUtil.mkdir(wp.toFile());
            String fileName = this.entityClass.getSimpleName() + suffix + ext;
            File file = wp.resolve(fileName).toFile();
            if (FileUtil.exist(file) && this.skip) {
                return null;
            }
            logger.info("自动生成代码: {}", file);
            return file;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void generate(CodeType codeType) throws Exception {
//        if (!ArrayUtil.contains(this.entityClass.getInterfaces(), ClassUtil.loadClass("com.jbm.framework.masterdata.usage.entity.MasterDataEntity"))) {
//            return;
//        }
        try {
            if (superclass == null) {
                logger.info("未检测到超类跳过:{}", this.entityClass);
                return;
            }
            if (this.ignore) {
                logger.info("设置为忽略生成:{}", this.entityClass);
                return;
            }
            Template t = buildData(codeType);
            if (t == null)
                return;
            File file = this.getWriteFile(codeType);
            if (file == null)
                return;
            t.renderTo(FileUtil.getOutputStream(file));
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage());
        } catch (Exception e) {
            throw e;
        }
    }


    public void generateAll() throws Exception {
        generate(mapperXml);
        generate(mapper);
        generate(service);
        generate(serviceImpl);
        generate(controller);
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
            Set<Class<?>> entitys = ClassUtil.scanPackage(entityPackage);
            logger.info("自动扫描生成代码:{},目标包名:{},发现{}个实体", entityPackage, targetPackage, entitys.size());
            for (Class clazz : entitys) {
                try {
                    GenerateMasterData generateMasterData = new GenerateMasterData(clazz, targetPackage);
                    generateMasterData.setSkip(true);
                    generateMasterData.generateAll();
                } catch (FileSystemNotFoundException e) {
                    //没找到文件就说明没有在开发环境
                    break;
                } catch (Exception e) {
                    logger.error("生成代码错误Class:{}", clazz, e);
                }
            }
        } catch (Exception e) {
            logger.error("生成代码错误", e);
        }
    }


}
