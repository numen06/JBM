package com.jbm.cluster.job.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.lang.Assert;
import com.jbm.cluster.api.entitys.job.DroolsRule;
import com.jbm.cluster.api.entitys.job.RuleOperationLog;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.job.business.impl.RuleReloadService;
import com.jbm.cluster.job.service.DroolsRuleService;
import com.jbm.cluster.job.service.RuleOperationLogService;
import com.jbm.cluster.job.util.DroolsUtil;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import jbm.framework.web.WebUtils;
import jodd.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public DroolsRule saveData(DroolsRule droolsRule){
        Assert.notNull(droolsRule.getRuleName(), "规则名称不能为空");
        Assert.notNull(droolsRule.getRuleContent(), "规则内容不能为空");
        DroolsUtil.checkRule(droolsRule.getRuleContent());

        droolsRule.setRuleCode("RULE_" + System.currentTimeMillis());
        super.saveEntity(droolsRule);
        //重新加载规则
        ruleReloadService.reloadRules();
        //增加操作日志
        String username = LoginHelper.getLoginUser().getUsername();
        String realName = LoginHelper.getLoginUser().getRealName();
        RuleOperationLog log = new RuleOperationLog();
        BeanUtil.copyProperties(droolsRule, log);
        log.setRuleId(droolsRule.getId());
        log.setOperationTime(DateTime.now());
        log.setOperationUser(username);
        log.setOperationUserName(realName);
        ruleOperationLogService.save(log);
        return droolsRule;
    }




}