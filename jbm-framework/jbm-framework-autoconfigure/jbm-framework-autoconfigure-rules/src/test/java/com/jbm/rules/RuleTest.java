package com.jbm.rules;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.mvel.MVELRule;
import org.jeasy.rules.support.composite.UnitRuleGroup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class RuleTest {

    @Test
    public void testOne() {
        //规则条件
        String condition = "name contains(\"张\")";
        String condition2 = "age > 10 && age <= 20";
        //规则引擎
        RulesEngine rulesEngine = new DefaultRulesEngine();
        Rules rules = new Rules();
        //具体规则，当满足condition，然后输出对应的success
        Rule rule = new MVELRule().when(condition).then("System.out.println(\"name success\")").priority(2);
        Rule rule2 = new MVELRule().when(condition2).then("System.out.println(\"age success\")").priority(1);

        rules.register(rule);
        rules.register(rule2);
        //匹配规则的事实
        Facts facts = new Facts();
        facts.put("name", "张");
        facts.put("age", "11");

        rulesEngine.fire(rules, facts);

    }

    @Test
    public void testTwo() {
        List<Result> list = new ArrayList<>();
        Result p1 = new Result();
        p1.setAge(11);
        p1.setName("张三");
        Result p2 = new Result();
        p2.setAge(12);
        p2.setName("张四");
        list.add(p1);
        list.add(p2);
        String condition = "person.name contains(\"张\")";
        String condition2 = "person.age > 10 && person.age <= 20";
        RulesEngine rulesEngine = new DefaultRulesEngine();
        Rules rules = new Rules();
        Rule rule = new MVELRule().when(condition).then("System.out.println(\"name success\")").priority(2);
        Rule rule2 = new MVELRule().when(condition2).then("System.out.println(\"age success\")").priority(1);
        rules.register(rule);
        rules.register(rule2);
        for (int i = 0; i < list.size(); i++) {
            Facts facts = new Facts();
            facts.put("person", list.get(i));
            rulesEngine.fire(rules, facts);
        }

    }


    @Test
    public void testThree() {
        List<Result> list = new ArrayList<>();
        Result p1 = new Result();
        p1.setAge(11);
        p1.setName("张三");
        Result p2 = new Result();
        p2.setAge(12);
        p2.setName("李四");
        list.add(p1);
        list.add(p2);
        String condition = "person.name contains(\"张\")";
        String condition2 = "person.age > 10 && person.age <= 20";
        RulesEngine rulesEngine = new DefaultRulesEngine();
        Rules rules = new Rules();

        Rule rule = new MVELRule().when(condition).then("System.out.println(\"name success\")").priority(2);
        Rule rule2 = new MVELRule().when(condition2).then("System.out.println(\"age success\")").priority(1);

        // 要么应用所有规则，要么什么都不应用。
        UnitRuleGroup unitRuleGroup = new UnitRuleGroup();
        unitRuleGroup.addRule(rule);
        unitRuleGroup.addRule(rule2);
        rules.register(unitRuleGroup);

        for (int i = 0; i < list.size(); i++) {
            Facts facts = new Facts();
            facts.put("person", list.get(i));
            rulesEngine.fire(rules, facts);
        }
    }
}
