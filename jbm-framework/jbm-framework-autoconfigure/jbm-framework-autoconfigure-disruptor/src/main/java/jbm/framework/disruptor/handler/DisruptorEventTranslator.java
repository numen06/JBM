package jbm.framework.disruptor.handler;

import com.lmax.disruptor.EventTranslator;
import jbm.framework.disruptor.core.EventProcesssBean;

import java.util.Date;

public class DisruptorEventTranslator implements EventTranslator<EventProcesssBean> {

    private final Object materials;

    public DisruptorEventTranslator(final Object materials) {
        super();
        this.materials = materials;
    }

    @Override
    public void translateTo(EventProcesssBean event, long sequence) {
        event.setSequence(sequence);
        event.setCreateTime(new Date());
        event.setSource(materials);
        // event.setTarget(product);
    }

}
