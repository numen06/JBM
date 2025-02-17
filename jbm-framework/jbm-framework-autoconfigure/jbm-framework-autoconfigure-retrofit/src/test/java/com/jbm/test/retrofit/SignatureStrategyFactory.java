package com.jbm.test.retrofit;

import com.jbm.test.retrofit.inf.SignatureStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class SignatureStrategyFactory {

    private Map<String, SignatureStrategy> signatureStrategies ;

    @Autowired
    private ApplicationContext applicationContext ;

    @PostConstruct
    public void init() {
        signatureStrategies = applicationContext.getBeansOfType(SignatureStrategy.class);
    }

    public SignatureStrategy getStrategy(String strategyName) {
        return signatureStrategies.get(strategyName);
    }
}