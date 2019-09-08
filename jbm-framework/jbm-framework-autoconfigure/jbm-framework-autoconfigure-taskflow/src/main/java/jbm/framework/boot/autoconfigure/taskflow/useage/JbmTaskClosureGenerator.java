package jbm.framework.boot.autoconfigure.taskflow.useage;

import com.ebay.bascomtask.config.ITaskClosureGenerator;
import com.ebay.bascomtask.main.TaskMethodClosure;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JbmTaskClosureGenerator implements ITaskClosureGenerator {


    @Override
    public TaskMethodClosure getClosure() {
        return new JbmTaskMethodClosure();
    }
}
