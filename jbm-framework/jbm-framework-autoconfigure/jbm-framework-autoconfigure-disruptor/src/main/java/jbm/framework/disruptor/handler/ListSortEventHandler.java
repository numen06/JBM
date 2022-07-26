package jbm.framework.disruptor.handler;

import jbm.framework.disruptor.core.DisruptorTemplate;
import jbm.framework.disruptor.core.EventProcesssBean;

import java.util.List;

/**
 * 将List分开推送到下级工厂
 *
 * @param <M>
 * @param <P>
 * @author wesley.zhang
 */
public class ListSortEventHandler<M, P> extends EventProcessHandler<M, P> {

    private final DisruptorTemplate<EventProcesssBean> disruptorTemplate;

    public ListSortEventHandler(DisruptorTemplate<EventProcesssBean> disruptorTemplate) {
        super();
        this.disruptorTemplate = disruptorTemplate;
    }

    @Override
    public P eventProcess(M materials) throws Exception {
        disruptorTemplate.getDisruptor().publishEvent(new DisruptorEventTranslator(materials));
        return null;
    }

    @Override
    public boolean eventIgnore(M materials) {
        if (materials instanceof List)
            return false;
        return true;
    }

}
