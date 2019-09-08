package jbm.framework.boot.autoconfigure.taskflow;

import jbm.framework.boot.autoconfigure.taskflow.useage.JbmTaskClosureGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JbmTaskFlowAutoConfiguration {

    @Bean
    public JbmTaskClosureGenerator jbmTaskClosureGenerator() {
        JbmTaskClosureGenerator jbmTaskClosureGenerator = new JbmTaskClosureGenerator();
        return jbmTaskClosureGenerator;
    }
}
