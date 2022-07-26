package com.jbm.test.stepchain.test.processor;

import com.github.zengfr.project.stepchain.AbstractStepProcessor;
import com.jbm.test.stepchain.test.SetProductRequest;
import com.jbm.test.stepchain.test.SetProductResponse;
import com.jbm.test.stepchain.test.context.SetProductContext;
import com.jbm.test.stepchain.test.context.SetProductDataMiddle;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeeProcessor extends AbstractStepProcessor<SetProductContext> {

    @Override
    protected Boolean execute(SetProductContext context) throws Exception {
        SetProductRequest req = context.left;
        SetProductResponse resp = context.right;
        SetProductDataMiddle middle = context.middle;
        middle.Price += 50;
        log.info(this.getClass().getName());
        return true;
    }

}
