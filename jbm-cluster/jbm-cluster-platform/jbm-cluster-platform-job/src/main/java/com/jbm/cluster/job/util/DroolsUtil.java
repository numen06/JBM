package com.jbm.cluster.job.util;

import com.jbm.framework.exceptions.ServiceException;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;

/**
 * @author scolin
 * @description
 * @date 2025/8/12 16:05
 */
public class DroolsUtil {
    /**
     * 校验规则
     * @param rule
     * @return
     */
    public static void checkRule(String rule){

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        try {
            kieFileSystem.write("src/main/resources/checkRule.drl", rule);
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                StringBuilder errorMsg = new StringBuilder("规则编译错误:\n");
                kieBuilder.getResults().getMessages().forEach(msg -> {
                    errorMsg.append(" - ").append(msg.getText()).append("\n");
                    errorMsg.append("   [规则: ").append(msg.getPath()).append(" 行: ")
                            .append(msg.getLine()).append("]\n");
                });
                throw new ServiceException(errorMsg.toString());
            }
        } catch (Exception e) {
            throw new ServiceException(e);
        } finally {
            kieFileSystem.delete("src/main/resources/checkRule.drl");
        }
    }
}
