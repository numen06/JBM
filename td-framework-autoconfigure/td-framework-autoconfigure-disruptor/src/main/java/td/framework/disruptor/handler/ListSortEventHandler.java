package td.framework.disruptor.handler;

import java.util.List;

import td.framework.disruptor.core.DisruptorTemplate;
import td.framework.disruptor.core.EventProcesssBean;

/**
 * 将List分开推送到下级工厂
 * 
 * @author wesley.zhang
 *
 * @param <M>
 * @param <P>
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
