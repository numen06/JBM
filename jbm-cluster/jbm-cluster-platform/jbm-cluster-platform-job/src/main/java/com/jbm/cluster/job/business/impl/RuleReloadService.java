package com.jbm.cluster.job.business.impl;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.job.DroolsRule;
import com.jbm.cluster.api.entitys.job.DynamicClass;
import com.jbm.cluster.job.service.DroolsRuleService;
import com.jbm.cluster.job.service.DynamicClassService;
import com.jbm.framework.exceptions.ServiceException;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author scolin
 * @description
 * @date 2025/8/4 17:32
 */
@Service
public class RuleReloadService {
    //private final KieServices kieServices = KieServices.Factory.get();
    private final AtomicReference<KieContainer> kieContainerRef = new AtomicReference<>();
    private final Map<String, String> ruleChecksums = new ConcurrentHashMap<>();

    private KieContainer kieContainer;

    @Autowired
    private DroolsRuleService droolsRuleService;
    @Autowired
    private LoadDynamicClassService loadDynamicClassService;

    @Autowired
    private DynamicClassService dynamicClassService;

    @PostConstruct
    public void init() throws Exception {
        //loadDynamicClassService.generateClass();
        reloadRules();
    }

    public synchronized void reloadRules() {
        // 1. 检查是否有变更
//        boolean needsReload = checkRuleChanges();
//        if (!needsReload) {
//            return;
//        }

        KieServices kieServices = KieServices.Factory.get();
        // 2. 构建新的规则容器
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        // 1. 生成并写入所有动态类
        //Map<String, byte[]> allClassBytes = writeDynamicClassesToKie(kieServices, kieFileSystem);

//        DroolsRule ruleParam = new DroolsRule();
//        ruleParam.setRuleStatus(true);

        QueryWrapper<DroolsRule> wrapper = new QueryWrapper<>();
        wrapper.eq("rule_status", true);
        wrapper.isNotNull("rule_content");

        List<DroolsRule> rules = droolsRuleService.selectEntitys(wrapper);
        rules.forEach(rule -> {
            String drlContent = processDynamicClassImports(rule.getRuleContent());
            kieFileSystem.write("src/main/resources/" + rule.getRuleCode() + rule.getVersion() + ".drl",
                    kieServices.getResources()
                            .newReaderResource(new StringReader(drlContent)));
        });

        // 3. 构建容器
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        //KieModule kieModule = kieBuilder.getKieModule();
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            StringBuilder errorMsg = new StringBuilder("规则编译错误:\n");
            kieBuilder.getResults().getMessages().forEach(msg -> {
                errorMsg.append(" - ").append(msg.getText()).append("\n");
                errorMsg.append("   [规则: ").append(msg.getPath()).append(" 行: ")
                        .append(msg.getLine()).append("]\n");
            });
            throw new ServiceException(errorMsg.toString());
        }

        // 4. 获取内部 ReleaseId 并创建 KieContainer
        ReleaseId releaseId = kieBuilder.getKieModule().getReleaseId();
        KieContainer kieContainer = kieServices.newKieContainer(releaseId);

        // 5. 原子更新容器
        kieContainerRef.set(kieServices.newKieContainer(releaseId));
    }

    private Map<String, byte[]> writeDynamicClassesToKie(KieServices kieServices, KieFileSystem kieFileSystem)
            throws Exception {

        Map<String, byte[]> allClassBytes = loadDynamicClassService.getAllClassBytes();

        // 写入每个动态类到KieFileSystem
        for (Map.Entry<String, byte[]> entry : allClassBytes.entrySet()) {
            String fullClassName = entry.getKey();
            byte[] bytecode = entry.getValue();

            // 转换为类路径
            String classPath = fullClassName.replace('.', '/') + ".class";
            String kiePath = "src/main/resources/" + classPath;

            // 写入到Kie文件系统
            kieFileSystem.write(
                    kiePath,
                    kieServices.getResources().newByteArrayResource(bytecode)
            );

            System.out.println("动态类写入KieFileSystem: " + kiePath);
        }

        return allClassBytes;
    }

    private String processDynamicClassImports(String drlContent) {
        // 获取所有动态类定义
        List<DynamicClass> dynamicClasses = dynamicClassService.list();
        if(CollUtil.isEmpty(dynamicClasses)){
            return drlContent;
        }

        StringBuilder imports = new StringBuilder();
        imports.append("// 自动生成的import语句\n");

        // 为每个动态类添加import语句
        dynamicClasses.forEach(dynamicClass -> {
            if (dynamicClass.getPackageName() != null && !dynamicClass.getPackageName().isEmpty()) {
                imports.append("import ")
                        .append(dynamicClass.getPackageName())
                        .append(".")
                        .append(dynamicClass.getClassName())
                        .append(";\n");
            }
        });

        imports.append("\n");

        return imports + drlContent;
    }



    private boolean checkRuleChanges() {
        List<DroolsRule> rules = droolsRuleService.list();

        // 规则数量变化
        if (rules.size() != ruleChecksums.size()) {
            return true;
        }

        // 规则内容变化
        return rules.stream().anyMatch(rule -> {
            String currentChecksum = ruleChecksums.get(rule.getRuleCode());
            String newChecksum = DigestUtils.md5DigestAsHex(rule.getRuleContent().getBytes());
            return !newChecksum.equals(currentChecksum);
        });
    }

    public KieSession newKieSession() {
        KieContainer container = kieContainerRef.get();
        if (container == null) {
            throw new IllegalStateException("规则容器未初始化");
        }
        return container.newKieSession();
    }
}
