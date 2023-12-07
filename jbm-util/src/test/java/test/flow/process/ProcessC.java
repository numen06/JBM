package test.flow.process;

import com.jbm.util.flow.AbstractFlowProcess;

public class ProcessC extends AbstractFlowProcess<String, Void> {

    /**
     * @param source
     * @return
     */
    @Override
    public Void doProcess(String source) {
        System.out.println(source);
        return null;
    }
}
