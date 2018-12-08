package jbm.framework.disruptor.handler;

import java.util.Date;

import com.lmax.disruptor.EventTranslator;

import jbm.framework.disruptor.core.EventProcesssBean;

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
