package jbm.framework.disruptor.handler;

import com.lmax.disruptor.EventHandler;
import jbm.framework.disruptor.core.EventProcesssBean;

public interface EventProcessFactory<M, P> extends EventHandler<EventProcesssBean> {

    /**
     * 事件加工
     *
     * @param object
     * @return
     * @throws Exception
     */
    public P eventProcess(M materials) throws Exception;

    /**
     * 生产过滤器
     *
     * @return
     */
    public boolean eventIgnore(M materials);

    /**
     * 提炼原材料
     *
     * @param source
     * @return
     */
    public M refiningMaterials(Object source);

    public void onException(Exception e, EventProcesssBean eventBean, M materials);

}
