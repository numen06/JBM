package com.jbm.cluster.job.service.impl.rule;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jbm.cluster.api.entitys.job.rule.RuleDefinition;
import com.jbm.cluster.api.entitys.job.rule.RuleOperationLog;
import com.jbm.cluster.api.form.job.DroolsParseAndExecuteForm;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.job.business.impl.RuleReloadService;
import com.jbm.cluster.job.service.rule.RuleDefinitionService;
import com.jbm.cluster.job.service.rule.RuleOperationLogService;
import com.jbm.cluster.job.util.DroolsUtil;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
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

/**
 * @Author: auto generate by jbm
 * @Create: 2025-08-12 14:03:24
 */
@Service
public class RuleDefinitionServiceImpl extends MasterDataServiceImpl<RuleDefinition> implements RuleDefinitionService {
    @Autowired
    RuleReloadService ruleReloadService;
    @Autowired
    private RuleOperationLogService ruleOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuleDefinition saveData(RuleDefinition ruleDefinition) {

        if (ruleDefinition.getId() == null) {
            //新增时校验
            Assert.notNull(ruleDefinition.getRuleName(), () -> new ServiceException("规则名称不能为空"));
            //如果前端未传规则code，则自动生成
            if (StrUtil.isBlank(ruleDefinition.getRuleCode())) {
                ruleDefinition.setRuleCode("RULE_" + System.currentTimeMillis());
            }
            //初始化版本号
            if (StrUtil.isBlank(ruleDefinition.getVersion())) {
                ruleDefinition.setVersion("1.0.0");
            }
        }else {
            //根据id查询是否存在
            RuleDefinition ruleDefinitionOld = super.getById(ruleDefinition.getId());
            Assert.notNull(ruleDefinitionOld, () -> new ServiceException("该id查询不到规则"));
        }
        //通过原始json内容解析出drools内容
        if (StrUtil.isNotBlank(ruleDefinition.getRuleContent())) {
            JSONArray jsonArray = compileRule(ruleDefinition.getRuleContent(),null);
            if(!jsonArray.isEmpty()){
                ruleDefinition.setDroolsContent(jsonArray.toString());
            }
        }

        super.saveEntity(ruleDefinition);
        //校验规则
        if (StrUtil.isNotBlank(ruleDefinition.getDroolsContent())) {
            JSONArray jsonArray = JSONUtil.parseArray(ruleDefinition.getDroolsContent());
            for (Object o : jsonArray) {
                JSONObject jsonObject = new JSONObject(o);
                String drools = jsonObject.get("drools").toString();
                DroolsUtil.checkRule(drools);
            }
        }

        //增加操作日志
        RuleDefinition copyRule = super.getById(ruleDefinition.getId());
        String username = LoginHelper.getLoginUser().getUsername();
        String realName = LoginHelper.getLoginUser().getRealName();
        RuleOperationLog log = new RuleOperationLog();
        BeanUtil.copyProperties(copyRule, log);
        log.setId(null);
        log.setRuleId(ruleDefinition.getId());
        log.setOperationTime(DateTime.now());
        log.setOperationUser(username);
        log.setOperationUserName(realName);
        ruleOperationLogService.save(log);
        return ruleDefinition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuleDefinition updateVersion(RuleDefinition ruleDefinition) {
        Assert.notNull(ruleDefinition.getId(), "规则id不能为空");
        Assert.notNull(ruleDefinition.getRuleCode(), "规则code不能为空");

        ruleDefinition = super.getById(ruleDefinition.getId());
        if (ruleDefinition == null) {
            throw new ServiceException("该id查询不到规则");
        }


        RuleDefinition param = new RuleDefinition();
        param.setRuleCode(ruleDefinition.getRuleCode());
        List<RuleDefinition> ruleDefinitions = super.selectEntitys(param);
        //取 droolsRules 版本号最大的一条数据
        RuleDefinition ruleDefinitionMax = ruleDefinitions.stream().max(Comparator.comparing(RuleDefinition::getVersion)).isPresent() ? ruleDefinitions.stream().max(Comparator.comparing(RuleDefinition::getVersion)).get() : null;
        if (ruleDefinitionMax == null) {
            throw new ServiceException("查询规则版本失败");
        }

        // 升版
        String currentVersion = ruleDefinitionMax.getVersion();
        String[] parts = currentVersion.split("\\.");
        int patch = Integer.parseInt(parts[2]) + 1;
        String newVersion = parts[0] + "." + parts[1] + "." + patch;
        ruleDefinition.setVersion(newVersion);
        ruleDefinition.setId(null);
        ruleDefinition.setCreateTime(null);
        ruleDefinition.setUpdateTime(null);
        this.saveData(ruleDefinition);
        return ruleDefinition;
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

                if (StrUtil.isNotBlank(nodeId)) {
                    if (!currNodeId.equals(nodeId)) {
                        continue;
                    }
                }

                if (StrUtil.isBlank(drools)) {
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
    public JSONObject parseNextNode(DroolsParseAndExecuteForm droolsParseAndExecuteForm) {
        Assert.notNull(droolsParseAndExecuteForm.getOriginalJson(), "原始json内容不能为空");
        Assert.notNull(droolsParseAndExecuteForm.getNodeId(), "节点ID不能为空");
        //获取当前节点的信息
        JSONObject jsonObj = JSONUtil.parseObj(droolsParseAndExecuteForm.getOriginalJson());
        JSONArray nodesArr = JSONUtil.parseArray(jsonObj.get("nodes"));
        JSONObject currNode = nodesArr.stream()
                .map(JSONUtil::parseObj)
                .filter(node -> node.getStr("id").equals(droolsParseAndExecuteForm.getNodeId()))
                .findFirst()
                .orElse(null);
        Assert.notNull(currNode, "当前节点不存在");
        JSONObject nextObj = getNextNode(droolsParseAndExecuteForm);
        Assert.notNull(currNode, "查询下一个节点失败");
        JSONObject jsonResult = new JSONObject();
        JSONObject nextNode = JSONUtil.parseObj(nextObj.get("nextNode"));
        if("conditions".equals(nextNode.get("type"))){
            //下个节点是规则节点
            jsonResult.set("nextRuleNode", nextNode);
            //规则节点解析出下下个节点 todo 当前只考虑一次规则节点，如果下下个节点还是规则节点怎么处理？
            droolsParseAndExecuteForm.setNodeId((String) nextNode.get("id"));
            JSONObject ruleResult = parseAndExecuteRule(droolsParseAndExecuteForm);
            JSONObject nextNextNode = JSONUtil.parseObj(ruleResult.get("nextNode"));
            jsonResult.set("nextNotRuleNode",nextNextNode);
            return jsonResult;
        }else {
            jsonResult.set("nextNotRuleNode", nextNode);
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
                JSONObject  res = getRuleNextNode(droolsParseAndExecuteForm,event.getMatch().getRule().getName());
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
     * 解析下个节点(当前节点是rule节点)
     *
     * @param droolsParseAndExecuteForm
     * @param ruleName
     * @return
     */
    public JSONObject getRuleNextNode(DroolsParseAndExecuteForm droolsParseAndExecuteForm,String ruleName){
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

    /**
     * 解析下个节点
     *
     * @param droolsParseAndExecuteForm
     * @return
     */
    public JSONObject getNextNode(DroolsParseAndExecuteForm droolsParseAndExecuteForm){
        Assert.notNull(droolsParseAndExecuteForm.getOriginalJson(), "原始json内容不能为空");
        JSONObject jsonResult = new JSONObject();

        JSONObject jsonObj = JSONUtil.parseObj(droolsParseAndExecuteForm.getOriginalJson());
        Object edgesObj = jsonObj.get("edges");
        JSONArray edges = JSONUtil.parseArray(edgesObj);
        //下一节点id
        String targetId = "";
        for (Object edge : edges) {
            JSONObject edgeObj = JSONUtil.parseObj(edge);
            Object source = edgeObj.get("source");

            String sourceId = source.toString();
            if(sourceId.equals(droolsParseAndExecuteForm.getNodeId())){
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