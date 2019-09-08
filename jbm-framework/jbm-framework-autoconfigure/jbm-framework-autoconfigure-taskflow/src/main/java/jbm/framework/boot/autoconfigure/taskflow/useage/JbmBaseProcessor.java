package jbm.framework.boot.autoconfigure.taskflow.useage;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public abstract class JbmBaseProcessor<Source extends Object> {

    /**
     * 目标的流程
     */
    private Class<JbmBaseProcessor> next;

    private boolean stop = false;


    public abstract void execute(Source source);

}
