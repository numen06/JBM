package com.jbm.cluster.job.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Assert;
import com.jbm.cluster.api.entitys.job.DroolsRule;
import com.jbm.cluster.api.entitys.job.RuleOperationLog;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.job.business.impl.RuleReloadService;
import com.jbm.cluster.job.service.DroolsRuleService;
import com.jbm.cluster.job.service.RuleOperationLogService;
import com.jbm.cluster.job.util.DroolsUtil;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import jodd.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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

        if(droolsRule.getId() == null){
            //新增时校验
            Assert.notNull(droolsRule.getRuleName(), ()-> new ServiceException("规则名称不能为空"));
            //如果前端未传规则code，则自动生成
            if(StringUtil.isBlank(droolsRule.getRuleCode())){
                droolsRule.setRuleCode("RULE_" + System.currentTimeMillis());
            }
            //初始化版本号
            if(StringUtil.isBlank(droolsRule.getVersion())){
                droolsRule.setVersion("1.0.0");
            }
        }

        super.saveEntity(droolsRule);
        //重新加载规则
        if(StringUtil.isNotEmpty(droolsRule.getRuleContent())){
           // DroolsUtil.checkRule(droolsRule.getRuleContent());
           // ruleReloadService.reloadRules();
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
    public DroolsRule updateVersion(DroolsRule droolsRule){
        Assert.notNull(droolsRule.getId(), "规则id不能为空");
        Assert.notNull(droolsRule.getRuleCode(), "规则code不能为空");

        droolsRule = super.getById(droolsRule.getId());
        if(droolsRule == null){
            throw new ServiceException("该id查询不到规则");
        }


        DroolsRule param = new DroolsRule();
        param.setRuleCode(droolsRule.getRuleCode());
        List<DroolsRule> droolsRules = super.selectEntitys(param);
        //取 droolsRules 版本号最大的一条数据
        DroolsRule droolsRuleMax = droolsRules.stream().max(Comparator.comparing(DroolsRule::getVersion)).isPresent() ? droolsRules.stream().max(Comparator.comparing(DroolsRule::getVersion)).get() : null;
        if(droolsRuleMax == null){
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

}