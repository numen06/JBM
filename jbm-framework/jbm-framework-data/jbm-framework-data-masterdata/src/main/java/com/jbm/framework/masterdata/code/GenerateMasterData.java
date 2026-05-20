package com.jbm.framework.masterdata.code;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.masterdata.code.annotation.BussinessGroup;
import com.jbm.framework.masterdata.code.annotation.IgnoreGeneate;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.generate.*;
import com.jbm.framework.masterdata.code.model.AutocodeYaml;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.code.model.GeneratedRecord;
import cn.hutool.setting.yaml.YamlUtil;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.util.template.simple.SimpleTemplateEngine;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Masterdata 代码生成入口。
 * <p>
 * Liquibase 全量导出：在已有库上可使用官方 CLI 或 liquibase-maven-plugin 执行 {@code generateChangeLog}，
 * 将 {@code changeLogFile} 指向 {@code src/main/resources/db/changelog/V0__initial_schema.xml}（或 YAML），
 * 与 {@code spring.liquibase.change-log} 主文件通过 {@code include} 衔接。
 * 若曾用 JPA/Hibernate 仅做一次性建表对齐，导出完成后应关闭 JPA 的 {@code ddl-auto}，以 Liquibase 为唯一结构真源。
 * 运行时依赖方面，{@code liquibase-core} 由 {@code jbm-framework-autoconfigure-mybatis} 与 MyBatis-Plus 同轨引入，业务模块无需重复声明。
 * </p>
 *
 * @author: create by wesley
 * @date:2019/4/18
 */
@Slf4j
@Data
public class GenerateMasterData {

//    private GroupTemplate groupTemplate;

    private TemplateEngine templateEngine;

    private String targetPackage;

    private String serviceModule;

    private String mapperModule;

    private String mapperXmlModule;

    private String serviceImplModule;

    private String controllerModule;

    private String businessModule;

    private String businessImplModule;

    private String mapperPackage;

    private String mapperXmlDir;

    private String servicePackage;

    private String controllerPackage;

    private String businessPackage;

    /**
     * 是否生成 Mapper 模块（mapper + mapperXml），默认 true
     */
    private boolean enableMapper = true;
    /**
     * 是否生成 Service 模块（service + serviceImpl），默认 true
     */
    private boolean enableService = true;
    /**
     * 是否生成 Controller 模块，默认 true
     */
    private boolean enableController = true;
    /**
     * 是否生成 Business 模块（business + businessImpl），默认 true
     */
    private boolean enableBusiness = true;

    /**
     * 标注了 @EnableCodeAutoGeneate 的启动类全限定名，用于将 autocode.yml 生成到该程序所在项目根
     */
    private String codeGenApplicationClass;

    private final List<IGenerateCode> generateCodeList = new ArrayList<>();

    /**
     * 当次扫描按实体对应的生成记录（entity -> 该实体生成的文件列表），用于写入 autocode.yml
     */
    private final Map<String, List<GeneratedRecord>> generatedRecordsByEntity = new LinkedHashMap<>();

    /**
     * 当次扫描是否实际写入了至少一个生成文件（已存在跳过的文件不计入）
     */
    private boolean anyNewlyGenerated = false;

    private final static String MASTERCARD_TEMP_PATH = "com/jbm/framework/masterdata/code/btl/";
    private static final String TIMESTAMP_ALREADY_EXISTED = "alreadyExisted";
    private static final String AUTOCODE_DIR = ".autocode";
    private static final String AUTOCODE_FILENAME = "autocode.yml";
    private static final String POM_XML = "pom.xml";

    public GenerateMasterData() {
        try {
            //构建模板引擎
            TemplateConfig templateConfig = new TemplateConfig(MASTERCARD_TEMP_PATH, TemplateConfig.ResourceMode.CLASSPATH);
            templateConfig = templateConfig.setCustomEngine(SimpleTemplateEngine.class);
            templateEngine = TemplateUtil.createEngine(templateConfig);

//            ClassPathResource resourceLoader = new ClassPathResource("/com/jbm/framework/masterdata/code/btl");
//            Configuration cfg = Configuration.defaultConfiguration();
//            groupTemplate = new GroupTemplate(resourceLoader, cfg);
            generateCodeList.add(new GenerateMapperCode());
            generateCodeList.add(new GenerateMapperXmlCode());
            generateCodeList.add(new GenerateServiceCode());
            generateCodeList.add(new GenerateServiceImplCode());
            generateCodeList.add(new GenerateBusinessCode());
            generateCodeList.add(new GenerateBusinessImplCode());
            generateCodeList.add(new GenerateControllerCode());


        } catch (Exception e) {
            log.error("初始化代码构建器失败");
        }
    }

    private final Map<Class<?>, List<Class<?>>> businessGroups = Maps.newConcurrentMap();

    public List<GenerateSource> filter(Set<Class<?>> entitys) throws Exception {
        List<GenerateSource> generateSourceList = new ArrayList<>();
        entitys.forEach(new Consumer<Class<?>>() {
            @Override
            public void accept(Class<?> entity) {
                GenerateSource generateSource = buildSource(entity, targetPackage);
                if (generateSource.getBussinessGroup() != null) {
                    if (businessGroups.containsKey(generateSource.getBussinessGroup().businessClass())) {
                        businessGroups.get(generateSource.getBussinessGroup().businessClass()).add(entity);
                    } else {
                        businessGroups.put(generateSource.getBussinessGroup().businessClass(), Lists.newArrayList(entity));
                    }
                }
                generateSource.setServiceModule(StrUtil.trimToNull(serviceModule));
                generateSource.setMapperModule(StrUtil.trimToNull(mapperModule));
                generateSource.setMapperXmlModule(StrUtil.trimToNull(mapperXmlModule));
                generateSource.setServiceImplModule(StrUtil.trimToNull(serviceImplModule));
                generateSource.setControllerModule(StrUtil.trimToNull(controllerModule));
                generateSource.setBusinessModule(StrUtil.trimToNull(businessModule));
                generateSource.setBusinessImplModule(StrUtil.trimToNull(businessImplModule));
                generateSource.setMapperPackage(StrUtil.trimToNull(mapperPackage));
                String xmlDir = StrUtil.trimToNull(mapperXmlDir);
                generateSource.setMapperXmlDir(xmlDir);
                if (xmlDir != null) {
                    generateSource.getData().put("mapperXmlDir", xmlDir);
                }
                generateSource.setServicePackage(StrUtil.trimToNull(servicePackage));
                generateSource.setControllerPackage(StrUtil.trimToNull(controllerPackage));
                generateSource.setBusinessPackage(StrUtil.trimToNull(businessPackage));
                generateSourceList.add(generateSource);
            }
        });
        return generateSourceList;
    }

    public GenerateSource generate(GenerateSource generateSource) throws Exception {
        for (IGenerateCode iGenerateCode : this.generateCodeList) {
            if (!isModuleEnabled(iGenerateCode.getCodeType())) {
                log.debug("跳过[{}]生成: 模块已禁用", iGenerateCode.getCodeType().name());
                continue;
            }
            try {
                Template template = templateEngine.getTemplate(iGenerateCode.getTemplateName(generateSource) + ".btl");
                generateSource.setTemplate(template);
                if (generateSource.getBussinessGroup() != null) {
                    // 将业务范围加入模板（使用 Map 列表避免模板引擎对 Class 反射，兼容 Java 9+ 模块）
                    List<Class<?>> entityClasses = businessGroups.get(generateSource.getBussinessGroup().businessClass());
                    List<Map<String, String>> bussinessEntityList = new ArrayList<>();
                    if (entityClasses != null) {
                        for (Class<?> c : entityClasses) {
                            Map<String, String> m = new HashMap<>();
                            m.put("simpleName", c.getSimpleName());
                            m.put("name", c.getName());
                            bussinessEntityList.add(m);
                        }
                    }
                    generateSource.getData().put("bussinessEntityList", bussinessEntityList);
                }
                try {
                    iGenerateCode.pre(generateSource);
                } catch (Exception e) {
                    log.debug("跳过[{}]生成:[{}],原因:{}", iGenerateCode.getCodeType().name(), generateSource.getEntityClass(), e.getMessage());
                    // 即使跳过生成（如文件已存在），也记录对应的生成信息，便于 autocode.yml 保留完整 entity→文件 对应关系
                    File targetFile = generateSource.getTargetFile();
                    if (targetFile != null) {
                        String path = targetFile.getAbsolutePath();
                        String entityName = generateSource.getEntityClass().getName();
                        GeneratedRecord record = GeneratedRecord.of(
                                entityName,
                                iGenerateCode.getCodeType(),
                                path,
                                TIMESTAMP_ALREADY_EXISTED
                        );
                        generatedRecordsByEntity.computeIfAbsent(entityName, k -> new ArrayList<>()).add(record);
                    }
                    continue;
                }
                File file = iGenerateCode.generate(generateSource);
                if (MapUtil.isNotEmpty(generateSource.getData())) {
                    generateSource.getData().putAll(generateSource.getData());
                }
                template.render(generateSource.getData(), file);
                anyNewlyGenerated = true;
                String entityName = generateSource.getEntityClass().getName();
                GeneratedRecord record = GeneratedRecord.of(
                        entityName,
                        iGenerateCode.getCodeType(),
                        file.getAbsolutePath(),
                        DateUtil.now()
                );
                generatedRecordsByEntity.computeIfAbsent(entityName, k -> new ArrayList<>()).add(record);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return generateSource;
    }

    /**
     * 将当次扫描已收集的生成记录写入与 pom.xml 平级目录下的 autocode.yml。
     * 若本次无新增生成且实体与路径映射未变，则跳过写入，避免每次启动改动 autocode.yml。
     * 不抛异常，仅打 log；写完后清空列表。
     */
    public void writeRecordFile() {
        try {
            File projectRoot = resolveAutocodeProjectRoot();
            File autocodeDir = new File(projectRoot, AUTOCODE_DIR);
            File recordFile = new File(autocodeDir, AUTOCODE_FILENAME);
            Set<String> currentEntityNames = generatedRecordsByEntity.keySet();
            Map<String, List<GeneratedRecord>> previousRecords = recordFile.isFile()
                    ? parseAutocodeYaml(recordFile) : new LinkedHashMap<>();
            if (recordFile.isFile()) {
                for (String entity : previousRecords.keySet()) {
                    if (!currentEntityNames.contains(entity)) {
                        for (GeneratedRecord r : previousRecords.get(entity)) {
                            if (StrUtil.isNotBlank(r.getPath())) {
                                File f = resolvePath(projectRoot, r.getPath());
                                if (f != null && FileUtil.del(f)) {
                                    log.debug("已删除已移除实体对应的生成文件: {}", r.getPath());
                                }
                            }
                        }
                    }
                }
            }
            mergePreviousTimestamps(projectRoot, previousRecords);
            if (!anyNewlyGenerated && recordFile.isFile()
                    && recordsContentEqual(projectRoot, previousRecords, generatedRecordsByEntity)) {
                log.debug("无新增生成且记录未变，跳过写入 autocode.yml");
                return;
            }
            String yaml = buildAutocodeYaml(projectRoot);
            FileUtil.mkdir(recordFile.getParentFile());
            Files.write(recordFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
            log.debug("已写入生成记录: {}", recordFile.getAbsolutePath());
        } catch (Exception e) {
            log.warn("写入 autocode.yml 失败: {}", e.getMessage());
        } finally {
            generatedRecordsByEntity.clear();
            anyNewlyGenerated = false;
        }
    }

    /**
     * 对已存在、未重新生成的条目，沿用 autocode.yml 中的 timestamp，避免无意义刷新。
     */
    private void mergePreviousTimestamps(File projectRoot,
                                         Map<String, List<GeneratedRecord>> previousRecords) {
        if (previousRecords.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<GeneratedRecord>> entry : generatedRecordsByEntity.entrySet()) {
            List<GeneratedRecord> prevList = previousRecords.get(entry.getKey());
            if (prevList == null) {
                continue;
            }
            for (GeneratedRecord current : entry.getValue()) {
                if (!TIMESTAMP_ALREADY_EXISTED.equals(current.getTimestamp())) {
                    continue;
                }
                for (GeneratedRecord prev : prevList) {
                    if (StrUtil.equals(prev.getCodeType(), current.getCodeType())
                            && StrUtil.equals(
                            toRelativePath(projectRoot, prev.getPath()),
                            toRelativePath(projectRoot, current.getPath()))) {
                        current.setTimestamp(prev.getTimestamp());
                        break;
                    }
                }
            }
        }
    }

    /**
     * 比较两次扫描的 entity→(codeType→path) 是否一致（忽略 timestamp、generatedAt）。
     */
    private boolean recordsContentEqual(File projectRoot,
                                        Map<String, List<GeneratedRecord>> previous,
                                        Map<String, List<GeneratedRecord>> current) {
        if (!previous.keySet().equals(current.keySet())) {
            return false;
        }
        for (String entity : previous.keySet()) {
            if (!indexByCodeType(projectRoot, previous.get(entity))
                    .equals(indexByCodeType(projectRoot, current.get(entity)))) {
                return false;
            }
        }
        return true;
    }

    private Map<String, String> indexByCodeType(File projectRoot, List<GeneratedRecord> records) {
        Map<String, String> indexed = new LinkedHashMap<>();
        if (records == null) {
            return indexed;
        }
        for (GeneratedRecord r : records) {
            if (r.getCodeType() != null && StrUtil.isNotBlank(r.getPath())) {
                indexed.put(r.getCodeType(), toRelativePath(projectRoot, r.getPath()));
            }
        }
        return indexed;
    }

    /**
     * 解析 autocode.yml 应写入的项目根：优先使用标注了 @EnableCodeAutoGeneate 的启动类所在项目，否则按首条生成文件路径或 user.dir 上找 pom.xml。
     */
    private File resolveAutocodeProjectRoot() {
        if (StrUtil.isNotBlank(codeGenApplicationClass)) {
            try {
                File root = findProjectRootFromClass(codeGenApplicationClass);
                if (root != null) {
                    return root;
                }
            } catch (Exception e) {
                log.debug("按启动类解析项目根失败，回退到路径查找: {}", e.getMessage());
            }
        }
        String firstPath = getFirstRecordPath();
        if (firstPath != null) {
            return findProjectRoot(firstPath);
        }
        return findProjectRoot(System.getProperty("user.dir"));
    }

    /**
     * 从标注了 @EnableCodeAutoGeneate 的类所在位置向上查找包含 pom.xml 的目录。
     */
    static File findProjectRootFromClass(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        java.net.URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
        if (location == null) {
            return null;
        }
        File base = new File(location.toURI());
        if (base.isFile() && base.getName().endsWith(".jar")) {
            base = base.getParentFile();
        } else if (base.isFile()) {
            base = base.getParentFile();
        }
        if (base == null || !base.exists()) {
            return null;
        }
        return findProjectRoot(base.getAbsolutePath());
    }

    private String getFirstRecordPath() {
        for (List<GeneratedRecord> list : generatedRecordsByEntity.values()) {
            if (!list.isEmpty() && list.get(0).getPath() != null) {
                return list.get(0).getPath();
            }
        }
        return null;
    }

    /**
     * 从某路径（可为文件路径或目录路径）向上查找包含 pom.xml 的目录；找不到则回退 user.dir。
     */
    static File findProjectRoot(String anyPath) {
        File dir = new File(anyPath).isFile() ? new File(anyPath).getParentFile() : new File(anyPath);
        while (dir != null) {
            if (new File(dir, POM_XML).isFile()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        return new File(System.getProperty("user.dir"));
    }

    /**
     * 将绝对路径转为相对项目根的虚拟路径，统一用正斜杠以利于跨平台。
     */
    private static String toRelativePath(File projectRoot, String absolutePath) {
        if (StrUtil.isBlank(absolutePath)) return absolutePath;
        try {
            Path root = projectRoot.toPath().normalize();
            Path abs = Paths.get(absolutePath).normalize();
            String relative = root.relativize(abs).toString();
            return relative.replace(File.separatorChar, '/');
        } catch (Exception e) {
            return absolutePath;
        }
    }

    /**
     * 将 path 解析为实际 File。若 path 已是绝对路径（兼容旧 autocode.yml），则直接使用；否则为相对路径，用 projectRoot 拼接。
     */
    private static File resolvePath(File projectRoot, String path) {
        if (StrUtil.isBlank(path)) return null;
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        }
        return new File(projectRoot, path);
    }

    private String buildAutocodeYaml(File projectRoot) {
        AutocodeYaml autocode = new AutocodeYaml();
        autocode.setGeneratedAt(DateUtil.now());
        for (Map.Entry<String, List<GeneratedRecord>> e : generatedRecordsByEntity.entrySet()) {
            AutocodeYaml.AutocodeItem item = new AutocodeYaml.AutocodeItem();
            item.setEntity(e.getKey());
            for (GeneratedRecord r : e.getValue()) {
                AutocodeYaml.AutocodeGenerated g = new AutocodeYaml.AutocodeGenerated();
                g.setCodeType(r.getCodeType());
                g.setPath(toRelativePath(projectRoot, r.getPath()));
                g.setTimestamp(r.getTimestamp());
                item.getGenerated().add(g);
            }
            autocode.getItems().add(item);
        }
        StringWriter sb = new StringWriter();
        YamlUtil.dump(autocode, sb);
        String yaml = sb.toString();
        return "# 自动生成代码记录（扫描包时建立的实体与生成文件对应关系）\n" + yaml;
    }

    /**
     * 解析已有 autocode.yml，得到 entity -> 生成记录列表。使用 YamlUtil 反序列化并映射为 AutocodeYaml 实体。
     */
    private Map<String, List<GeneratedRecord>> parseAutocodeYaml(File recordFile) {
        Map<String, List<GeneratedRecord>> result = new LinkedHashMap<>();
        try {
            FileReader reader = new FileReader(recordFile);
            Object loaded = YamlUtil.load(reader);
            if (!(loaded instanceof Map)) {
                return result;
            }
            Map<?, ?> root = (Map<?, ?>) loaded;
            Object itemsObj = root.get("items");
            if (!(itemsObj instanceof List)) {
                return result;
            }
            for (Object itemObj : (List<?>) itemsObj) {
                if (!(itemObj instanceof Map)) continue;
                Map<?, ?> itemMap = (Map<?, ?>) itemObj;
                String entity = itemMap.get("entity") != null ? itemMap.get("entity").toString() : null;
                if (StrUtil.isBlank(entity)) continue;
                Object genList = itemMap.get("generated");
                if (!(genList instanceof List)) continue;
                List<GeneratedRecord> records = result.computeIfAbsent(entity, k -> new ArrayList<>());
                for (Object gObj : (List<?>) genList) {
                    if (!(gObj instanceof Map)) continue;
                    Map<?, ?> gMap = (Map<?, ?>) gObj;
                    String codeType = gMap.get("codeType") != null ? gMap.get("codeType").toString() : null;
                    String path = gMap.get("path") != null ? gMap.get("path").toString() : null;
                    String timestamp = gMap.get("timestamp") != null ? gMap.get("timestamp").toString() : null;
                    if (StrUtil.isNotBlank(path)) {
                        records.add(new GeneratedRecord(entity, codeType, path, timestamp));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("解析 autocode.yml 失败，跳过删除旧文件: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 按代码类型判断所属模块是否启用。mapper/mapperXml 属 mapper 模块，service/serviceImpl 属 service 模块，controller 属 controller 模块，business/businessImpl 属 business 模块。
     */
    private boolean isModuleEnabled(CodeType codeType) {
        if (codeType == null) return false;
        switch (codeType) {
            case mapper:
            case mapperXml:
                return enableMapper;
            case service:
            case serviceImpl:
                return enableService;
            case controller:
                return enableController;
            case business:
            case businessImpl:
                return enableBusiness;
            default:
                return true;
        }
    }

    public GenerateSource buildSource(Class<?> entityClass, String targetPackage) {
        GenerateSource generateSource = new GenerateSource();
        generateSource.setEntityClass(entityClass);
        generateSource.setTargetPackage(targetPackage);
        IgnoreGeneate ignoreGeneate = AnnotationUtil.getAnnotation(entityClass, IgnoreGeneate.class);
        generateSource.setIgnoreGeneate(ignoreGeneate);
        ApiModel apiModel = AnnotationUtil.getAnnotation(entityClass, ApiModel.class);
        generateSource.setApiModel(apiModel);
        BussinessGroup bussinessGroup = AnnotationUtil.getAnnotation(entityClass, BussinessGroup.class);
        generateSource.setBussinessGroup(bussinessGroup);
        //        generateSource.setTargetPackage(ClassUtil.getPackage(generateSource.getEntityClass()));
        if (MasterDataEntity.class.isAssignableFrom(generateSource.getEntityClass())) {
            generateSource.setSuperclass(generateSource.getEntityClass().getSuperclass());
        }
        if (generateSource.getSuperclass() == null) {
            log.debug("未检测到超类跳过:{}", entityClass);
        }
        return generateSource;
    }


}
