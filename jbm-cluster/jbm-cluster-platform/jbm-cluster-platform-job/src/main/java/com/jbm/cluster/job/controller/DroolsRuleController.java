package com.jbm.cluster.job.controller;

import com.jbm.cluster.api.entitys.job.DroolsRule;
import com.jbm.cluster.job.business.impl.LoadDynamicClassService;
import com.jbm.cluster.job.business.impl.RuleEngineService;
import com.jbm.cluster.job.service.DroolsRuleService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-08-12 14:03:24
 */
@Api(tags = "drools规则开放接口")
@RestController
@RequestMapping("/droolsRule")
public class DroolsRuleController extends MasterDataCollection<DroolsRule, DroolsRuleService> {
    @Autowired
    private RuleEngineService ruleEngineService;
    @Autowired
    private LoadDynamicClassService loadDynamicClassService;
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
        //order.setScore(1000.00);

        try {
            ruleEngineService.executeRules(map);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        return ResultBody.success("成功");
    }
}
