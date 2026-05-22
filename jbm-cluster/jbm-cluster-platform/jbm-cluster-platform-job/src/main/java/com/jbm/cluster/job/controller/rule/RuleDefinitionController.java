package com.jbm.cluster.job.controller.rule;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONObject;
import com.jbm.cluster.api.entitys.job.rule.RuleDefinition;
import com.jbm.cluster.api.entitys.message.drools.DroolsFeignTemplate;
import com.jbm.cluster.api.form.job.DroolsParseAndExecuteForm;
import com.jbm.cluster.api.service.IDroolsRuleServiceClient;
import com.jbm.cluster.job.business.impl.LoadDynamicClassService;
import com.jbm.cluster.job.business.impl.RuleEngineService;
import com.jbm.cluster.job.business.impl.RuleReloadService;
import com.jbm.cluster.job.service.rule.RuleDefinitionService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.form.IdsForm;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-08-12 14:03:24
 */
@Api(tags = "drools规则开放接口")
@RestController
@RequestMapping("/droolsRule")
public class RuleDefinitionController extends MasterDataCollection<RuleDefinition, RuleDefinitionService> implements IDroolsRuleServiceClient {
    @Autowired
    private RuleDefinitionService ruleDefinitionService;
    @Autowired
    private RuleEngineService ruleEngineService;
    @Autowired
    private LoadDynamicClassService loadDynamicClassService;
    @Autowired
    private RuleReloadService ruleReloadService;


    @ApiOperation("保存规则")
    @PostMapping("/saveData")
    public ResultBody<RuleDefinition> saveData(@RequestBody(required = false) RuleDefinition ruleDefinition) {
        return ResultBody.callback(() -> ruleDefinitionService.saveData(ruleDefinition));
    }

    @ApiOperation("升版")
    @PostMapping("/updateVersion")
    public ResultBody<RuleDefinition> updateVersion(@RequestBody(required = false) RuleDefinition ruleDefinition) {
        return ResultBody.callback(() -> ruleDefinitionService.updateVersion(ruleDefinition));
    }

    /**
     * 删除
     *
     * @param masterDataRequsetBody
     * @return
     */
    @ApiOperation(value = "删除实体", notes = "删除实体")
    @PostMapping("/delete")
    @Override
    public ResultBody<Boolean> remove(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            RuleDefinition entity = validatorMasterData(masterDataRequsetBody, true);
            ruleDefinitionService.deleteEntity(entity);
            //ruleReloadService.reloadRules();
        } catch (Exception e) {
            return ResultBody.error(e);
        }
        return ResultBody.success(true, "删除成功");
    }

    /**
     * 批量删除
     *
     * @param idsForm
     * @return
     */
    @ApiOperation(value = "通过ids批量删除实体", notes = "通过ids批量删除实体")
    @PostMapping("/deleteByIds")
    @Override
    public ResultBody<Boolean> deleteByIds(@RequestBody(required = false) IdsForm idsForm) {
        try {
            List<Long> ids = idsForm.getIds();
            if (CollectionUtil.isEmpty(ids)) {
                return ResultBody.error(true, "ids为空");
            }
            if (ruleDefinitionService.removeByIds(ids)) {
                //ruleReloadService.reloadRules();
                return ResultBody.success(false, "批量成功刪除");
            }
            return ResultBody.error(false, "批量成功刪除");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @PostMapping({"/execute"})
    @Override
    public ResultBody<DroolsFeignTemplate> executeRule(@RequestBody(required = false) DroolsFeignTemplate droolsFeignTemplate)  {
        Map<String, Object> factMap = droolsFeignTemplate.getFact();
        try {
            // 校验编号
            Assert.notNull(droolsFeignTemplate.getRuleCode(), "请传入规则编号");
            // 校验版本号
            Assert.notNull(droolsFeignTemplate.getVersion(), "请传入版本号");
            // 校验实例
            if (MapUtil.isEmpty(factMap)) {
                return ResultBody.error("请传入实例map");
            }
            //

            // 执行规则引擎
            Map<String, Object> result = ruleEngineService.executeRules(factMap);

            // 返回执行结果
            droolsFeignTemplate.setFact(result);
            return ResultBody.success(droolsFeignTemplate, "执行成功");
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @PostMapping({"/test1"})
    public ResultBody<String> test1(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody)  {
        System.out.println("测试开始-》》》》》》》》》》》》》》》》");


        try {
            //List<Class<?>> classes = loadDynamicClassService.getGeneratedClasses().stream().filter(clazz -> clazz.getName().equals("com.jajachina.rule.model.Order")).collect(Collectors.toList());

            Class<?> clazz = loadDynamicClassService.getGeneratedClasses().get(0);
            Object fact = clazz.getDeclaredConstructor().newInstance();
            Field amount = clazz.getDeclaredField("amount");
            amount.setAccessible(true);
            amount.setDouble(fact, 60.00);
            ruleEngineService.executeRules(fact);
        } catch (Exception e) {
            //log.error("test1 error", e);
            throw new RuntimeException(e);
        }
        return ResultBody.success("成功");
    }

    @PostMapping({"/test2"})
    public ResultBody<String> test2(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        System.out.println("测试2开始-》》》》》》》》》》》》》》》》");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("age",25);
        try {
            ruleEngineService.executeRules(map);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        return ResultBody.success("成功");
    }

    @PostMapping({"/test3"})
    public ResultBody<Object> test3(@RequestBody(required = false) Map<String, Object> factMap) {
        System.out.println("测试3开始-》》》》》》》》》》》》》》》》");

        try {
            // 如果没有传入参数，则使用默认值
            if (MapUtil.isEmpty(factMap)) {
                return ResultBody.error("请传入实例map");
            }

            // 执行规则引擎
            Object result = ruleEngineService.executeRules(factMap);

            // 返回执行结果
            return ResultBody.success(result, "执行成功");
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @PostMapping({"/parseAndExecuteRule"})
    @Override
    public ResultBody<JSONObject> parseAndExecuteRule(@RequestBody(required = false) DroolsParseAndExecuteForm droolsParseAndExecuteForm)  {
        try {
            JSONObject jsonObject = ruleDefinitionService.parseAndExecuteRule(droolsParseAndExecuteForm);
            return ResultBody.success(jsonObject,"成功");
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }


    @PostMapping({"/parseNextNode"})
    @Override
    public ResultBody<JSONObject> parseNextNode(@RequestBody(required = false) DroolsParseAndExecuteForm droolsParseAndExecuteForm)  {
        try {
            JSONObject jsonObject = ruleDefinitionService.parseNextNode(droolsParseAndExecuteForm);
            return ResultBody.success(jsonObject,"成功");
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }


}
