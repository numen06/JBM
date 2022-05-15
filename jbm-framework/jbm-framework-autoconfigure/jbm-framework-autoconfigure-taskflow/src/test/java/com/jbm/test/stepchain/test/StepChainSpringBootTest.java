package com.jbm.test.stepchain.test;

import com.github.zengfr.project.stepchain.IPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = StepChainTestApplication.class)
public class StepChainSpringBootTest {
    @Autowired
    protected IPipeline pipeline;

    @Test
    public void testPipeline() throws Exception {
        PipelineTest.testPipeline(pipeline);
    }

    @Test
    public void testPipeline2() throws Exception {
        PipelineTest.testPipeline2(pipeline);
    }
}