package com.jbm.cluster.job.business.impl;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author scolin
 * @description
 * @date 2025/8/4 17:54
 */
@Slf4j
@Service
public class RuleEngineService {
    @Autowired
    private RuleReloadService ruleReloadService;

    public <T> T executeRules(T fact) {
        KieSession kieSession = null;
        try {
            kieSession = ruleReloadService.newKieSession();
            kieSession.insert(fact);
            int res = kieSession.fireAllRules();
            if(res == 0){
                log.warn("{}没有匹配到规则", fact);
            }
            return fact;
        }catch(Exception  e){
            throw new RuntimeException(e);
        } finally {
            if (kieSession != null) {
                kieSession.dispose();
            }
        }
    }

    public <T> List<T> executeRules(List<T> facts) {
        // 类似实现，支持批量处理
        return facts;
    }
}
