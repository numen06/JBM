package com.jbm.cluster.job.execute;

import cn.hutool.json.JSONObject;
import com.jbm.cluster.api.model.job.rule.NodeData;
import com.jbm.cluster.job.business.impl.RuleReloadService;
import com.jbm.framework.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.event.DefaultAgendaEventListener;
import org.kie.api.KieServices;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 11:42
 */
@Component
@Slf4j
public class DroolsRuleEngine {
    // private final KieContainer kieContainer;
    @Resource
    private RuleReloadService ruleReloadService;

    public DroolsRuleEngine() {
        // KieServices kieServices = KieServices.Factory.get();
        // this.kieContainer = kieServices.getKieClasspathContainer();
    }

    public String evaluateCondition(NodeData conditionNode, Map<String, Object> inputData) {
        KieSession kieSession = null;
        try {
            Map<String, Object> nodeData = conditionNode.getData();
            String droolsRule = (String) ((Map<String, Object>) nodeData.get("rule")).get("drools");

            KieContainer kieContainer = ruleReloadService.addRulesForFlow(droolsRule, conditionNode.getId());

            // 创建KieSession
            kieSession = kieContainer.newKieSession();

            // 插入事实
            kieSession.insert(inputData);

            // 设置全局变量来捕获结果
            List<String> firedRules = new ArrayList<>();

            // 监听执行结果
            kieSession.addEventListener(new DefaultAgendaEventListener() {
                @Override
                public void afterMatchFired(AfterMatchFiredEvent event) {
                    super.afterMatchFired(event);
                    log.info("规则触发: {}", event.getMatch().getRule().getName());
                    log.info("触发事实: {}", event.getMatch().getFactHandles());
                    firedRules.add(event.getMatch().getRule().getName());
                }
            });

            // 执行规则
            kieSession.fireAllRules();
            kieSession.dispose();

            // 根据触发的规则决定分支
            if (!firedRules.isEmpty()) {
                // "Rule_60285_elseIf_39639" 这种类型字符串截取 最后一段 39639
                return firedRules.get(0).substring(firedRules.get(0).lastIndexOf("_") + 1);
            } else {
                // 没有规则被触发，返回 else 分支
                log.warn("条件节点{}没有任何规则被触发，将返回else分支", conditionNode.getId());
                return findElseBranchId(conditionNode);
            }

            // 默认返回else分支
            // return findElseBranchId(conditionNode);

        } catch (Exception e) {
            throw new ServiceException("规则执行失败: " + e.getMessage(), e);
        } finally {
            if (kieSession != null) {
                kieSession.dispose();
            }
        }
    }

    private String extractBranchIdFromRuleName(String ruleName) {
        // 从规则名中提取分支ID
        if (ruleName.contains("if")) {
            return "if";
        } else if (ruleName.contains("elseIf")) {
            return "elseIf";
        } else {
            return "else";
        }
    }

    private String findElseBranchId(NodeData conditionNode) {
        // 从节点数据中查找else分支的ID
        try {
            Map<String, Object> nodeData = conditionNode.getData();
            if (nodeData == null) {
                log.error("条件节点{}的data为null", conditionNode.getId());
                throw new ServiceException("条件节点配置错误：data为null");
            }

            Map<String, Object> ruleData = (Map<String, Object>) nodeData.get("rule");
            if (ruleData == null) {
                log.error("条件节点{}的rule为null", conditionNode.getId());
                throw new ServiceException("条件节点配置错误：rule为null");
            }

            List<Map<String, Object>> branches = (List<Map<String, Object>>) ruleData.get("branches");
            if (branches == null || branches.isEmpty()) {
                log.error("条件节点{}没有任何分支", conditionNode.getId());
                throw new ServiceException("条件节点没有配置分支");
            }

            String elseBranchId = branches.stream()
                    .filter(branch -> "else".equals(branch.get("type")))
                    .map(branch -> (String) branch.get("id"))
                    .findFirst()
                    .orElse(null);

            if (elseBranchId == null) {
                log.warn("条件节点{}没有else分支配置，列表分支: {}", conditionNode.getId(), branches);
                throw new ServiceException("条件节点没有else分支");
            }

            log.info("条件节点{}没有規則匹配，返回else分支: {}", conditionNode.getId(), elseBranchId);
            return elseBranchId;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("查找else分支失败", e);
            throw new ServiceException("查找else分支失败: " + e.getMessage());
        }
    }
}
