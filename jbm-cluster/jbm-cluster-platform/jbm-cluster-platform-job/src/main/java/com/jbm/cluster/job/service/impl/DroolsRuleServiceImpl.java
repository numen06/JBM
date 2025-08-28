package com.jbm.cluster.job.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jbm.cluster.api.entitys.job.DroolsRule;
import com.jbm.cluster.api.entitys.job.RuleOperationLog;
import com.jbm.cluster.api.form.job.DroolsParseAndExecuteForm;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.job.business.impl.RuleReloadService;
import com.jbm.cluster.job.service.DroolsRuleService;
import com.jbm.cluster.job.service.RuleOperationLogService;
import com.jbm.cluster.job.util.DroolsUtil;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import jodd.util.StringUtil;
import org.drools.core.event.DefaultAgendaEventListener;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-08-12 14:03:24
 */
@Service
public class DroolsRuleServiceImpl extends MasterDataServiceImpl<DroolsRule> implements DroolsRuleService {
    @Autowired
    RuleReloadService ruleReloadService;
    @Autowired
    private RuleOperationLogService ruleOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DroolsRule saveData(DroolsRule droolsRule) {

        if (droolsRule.getId() == null) {
            //新增时校验
            Assert.notNull(droolsRule.getRuleName(), () -> new ServiceException("规则名称不能为空"));
            //如果前端未传规则code，则自动生成
            if (StringUtil.isBlank(droolsRule.getRuleCode())) {
                droolsRule.setRuleCode("RULE_" + System.currentTimeMillis());
            }
            //初始化版本号
            if (StringUtil.isBlank(droolsRule.getVersion())) {
                droolsRule.setVersion("1.0.0");
            }
        }else {
            //根据id查询是否存在
            DroolsRule droolsRuleOld = super.getById(droolsRule.getId());
            Assert.notNull(droolsRuleOld, () -> new ServiceException("该id查询不到规则"));
        }
        //通过原始json内容解析出drools内容
        if (StringUtil.isNotBlank(droolsRule.getRuleContent())) {
            JSONArray jsonArray = compileRule(droolsRule.getRuleContent(),null);
            if(!jsonArray.isEmpty()){
                droolsRule.setDroolsContent(jsonArray.toString());
            }
        }

        super.saveEntity(droolsRule);
        //重新加载规则
        if (StringUtil.isNotEmpty(droolsRule.getDroolsContent())) {
            // DroolsUtil.checkRule(droolsRule.getRuleContent());
            ruleReloadService.reloadRules();
        }

        //增加操作日志
        DroolsRule copyRule = super.getById(droolsRule.getId());
        String username = LoginHelper.getLoginUser().getUsername();
        String realName = LoginHelper.getLoginUser().getRealName();
        RuleOperationLog log = new RuleOperationLog();
        BeanUtil.copyProperties(copyRule, log);
        log.setId(null);
        log.setRuleId(droolsRule.getId());
        log.setOperationTime(DateTime.now());
        log.setOperationUser(username);
        log.setOperationUserName(realName);
        ruleOperationLogService.save(log);
        return droolsRule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DroolsRule updateVersion(DroolsRule droolsRule) {
        Assert.notNull(droolsRule.getId(), "规则id不能为空");
        Assert.notNull(droolsRule.getRuleCode(), "规则code不能为空");

        droolsRule = super.getById(droolsRule.getId());
        if (droolsRule == null) {
            throw new ServiceException("该id查询不到规则");
        }


        DroolsRule param = new DroolsRule();
        param.setRuleCode(droolsRule.getRuleCode());
        List<DroolsRule> droolsRules = super.selectEntitys(param);
        //取 droolsRules 版本号最大的一条数据
        DroolsRule droolsRuleMax = droolsRules.stream().max(Comparator.comparing(DroolsRule::getVersion)).isPresent() ? droolsRules.stream().max(Comparator.comparing(DroolsRule::getVersion)).get() : null;
        if (droolsRuleMax == null) {
            throw new ServiceException("查询规则版本失败");
        }

        // 升版
        String currentVersion = droolsRuleMax.getVersion();
        String[] parts = currentVersion.split("\\.");
        int patch = Integer.parseInt(parts[2]) + 1;
        String newVersion = parts[0] + "." + parts[1] + "." + patch;
        droolsRule.setVersion(newVersion);
        droolsRule.setId(null);
        droolsRule.setCreateTime(null);
        droolsRule.setUpdateTime(null);
        this.saveData(droolsRule);
        return droolsRule;
    }


    /**
     * 前端传入的原始json解析drool内容
     *
     * @param originalJson
     * @return
     */
    @Override
    public JSONArray compileRule(String originalJson, String nodeId) {
        Assert.notBlank(originalJson, "流程内容不能为空");
        JSONArray jsonResult = new JSONArray();
        JSONObject jsonObj = JSONUtil.parseObj(originalJson);
        Object obj = jsonObj.get("nodes");
        JSONArray nodes = JSONUtil.parseArray(obj);

        //遍历nodes
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            String currNodeId = node.get("id").toString();
            if (node.get("type").equals("conditions")) {

                JSONObject jsonData = new JSONObject(node.get("data"));
                JSONObject jsonRule = new JSONObject(jsonData.get("rule"));
                if (jsonRule.get("drools") == null) {
                    continue;
                }
                String drools = jsonRule.get("drools").toString();

                if (StringUtil.isNotBlank(nodeId)) {
                    if (!currNodeId.equals(nodeId)) {
                        continue;
                    }
                }

                if (StringUtil.isBlank(drools)) {
                    throw new ServiceException("规则内容不能为空");
                }
                DroolsUtil.checkRule(drools);
                JSONObject jsonObject = new JSONObject();
                jsonObject.set("nodeId", nodeId);
                jsonObject.set("drools", drools);
                jsonResult.add(jsonObject);
            }
        }

        return jsonResult;
    }

    @Override
    public JSONObject parseAndExecuteRule(DroolsParseAndExecuteForm droolsParseAndExecuteForm) {
        Assert.notNull(droolsParseAndExecuteForm.getOriginalJson(), "原始json内容不能为空");
        Assert.notNull(droolsParseAndExecuteForm.getNodeId(), "节点ID不能为空");
        Assert.notNull(droolsParseAndExecuteForm.getFact(), "实例不能为空");
        JSONObject jsonResult = new JSONObject();
        JSONArray jsonArray = this.compileRule(droolsParseAndExecuteForm.getOriginalJson(), droolsParseAndExecuteForm.getNodeId());

        KieContainer kieContainer = ruleReloadService.addRules(jsonArray);

        KieSession kieSession = kieContainer.newKieSession();

        // 添加监听器
        kieSession.addEventListener(new DefaultAgendaEventListener() {
            @Override
            public void afterMatchFired(AfterMatchFiredEvent event) {
                super.afterMatchFired(event);
                System.out.println("规则触发: " + event.getMatch().getRule().getName());
                System.out.println("触发事实: " + event.getMatch().getFactHandles());
                JSONObject  res = getNextNode(droolsParseAndExecuteForm,event.getMatch().getRule().getName());
                //将res赋值给jsonResult
                jsonResult.putAll(res);
            }
        });

        //将 droolsParseAndExecuteForm.getFact()转成map
        Map<String, String> fact = JSONUtil.toBean(droolsParseAndExecuteForm.getFact(), Map.class);
        kieSession.insert(fact);
        //kieSession.getAgenda().getAgendaGroup("default").setFocus();
        int res = kieSession.fireAllRules();
        if (res == 0) {
            throw new ServiceException("没有匹配到规则");
        }
        kieSession.destroy();
        kieContainer.dispose();
        return jsonResult;
    }

    /**
     * 解析下个节点
     *
     * @param droolsParseAndExecuteForm
     * @param ruleName
     * @return
     */
    public JSONObject getNextNode(DroolsParseAndExecuteForm droolsParseAndExecuteForm,String ruleName){
        Assert.notNull(droolsParseAndExecuteForm.getOriginalJson(), "原始json内容不能为空");
        Assert.notNull(ruleName, "规则名称不能为空");
        JSONObject jsonResult = new JSONObject();
        //截取规则名称的最后一段，代表当前ifElse节点的id
        //比如 ruleName = "Rule_21368_elseif_31014" 要取 31014
        String ruleNodeId = ruleName.substring(ruleName.lastIndexOf("_") + 1);
        //再用 ruleNodeId 去 originalJson 中查找下一个节点
        JSONObject jsonObj = JSONUtil.parseObj(droolsParseAndExecuteForm.getOriginalJson());
        Object edgesObj = jsonObj.get("edges");
        JSONArray edges = JSONUtil.parseArray(edgesObj);
        //下一节点id
        String targetId = "";
        for (Object edge : edges) {
            JSONObject edgeObj = JSONUtil.parseObj(edge);
            Object sourceHandle = edgeObj.get("sourceHandle");
            if(sourceHandle == null){
                continue;
            }
            //截取 sourceHandle 第2段，代表当前节点的 id
            //比如 sourceHandle = "conditions-31014-elseIf-source-handle" 要取 31014
            String sourceHandleId = sourceHandle.toString().split("-")[1];
            if(sourceHandleId.equals(ruleNodeId)){
                //对应的连线信息
                targetId = edgeObj.get("target").toString();
                jsonResult.set("edgeObj",edgeObj);
                break;
            }
        }
        Object nodesObj = jsonObj.get("nodes");
        JSONArray nodes = JSONUtil.parseArray(nodesObj);
        String finalTargetId = targetId;
        nodes.stream().filter(node -> {
            JSONObject nodeObj = JSONUtil.parseObj(node);
            return nodeObj.get("id").toString().equals(finalTargetId);
        }).findFirst().ifPresent(node -> {
            JSONObject nodeObj = JSONUtil.parseObj(node);
            jsonResult.set("nextNode",nodeObj);
        });

        return jsonResult;
    }

}