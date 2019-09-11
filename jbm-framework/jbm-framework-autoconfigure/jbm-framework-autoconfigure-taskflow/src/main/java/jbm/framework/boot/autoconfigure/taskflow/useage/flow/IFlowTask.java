package jbm.framework.boot.autoconfigure.taskflow.useage.flow;

/**
 * 流程编封装接口
 */
public interface IFlowTask {

    /**
     * 随后执行
     *
     * @param jbmFlow
     * @return
     */
    IFlowTask after(IFlowTask jbmFlow);


    /**
     * 并行执行
     *
     * @param jbmFlow
     * @return
     */
    IFlowTask add(IFlowTask jbmFlow);


    /**
     * 行动
     */
    void action();

}
