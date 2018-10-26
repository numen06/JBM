package jbm.framework.disruptor.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jbm.framework.disruptor.core.EventProcesssBean;

public abstract class EventProcessHandler<M, P> implements EventProcessFactory<M, P> {

	protected static Logger logger = LoggerFactory.getLogger(EventProcessHandler.class);

	// private ConcurrentMap<Long, EventProcesssBean> eventBeanMap = new
	// ConcurrentHashMap<Long, EventProcesssBean>();

	public EventProcessHandler() {
		super();
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Override
	public final void onEvent(EventProcesssBean eventBean, long sequence, boolean endOfBatch) throws Exception {
		if (eventBean == null)
			return;
		M materials = null;
		try {
			// eventBeanMap.put(sequence, eventBean);
			// event.setSequence(sequence);
			eventBean.setEndOfBatch(endOfBatch);
			// this.materials(event.getSource());
			// event.setTarget(this.product());
			boolean ingore = false;
			if (eventBean.getSource() == null)
				return;
			materials = this.refiningMaterials(eventBean.getSource());
			try {
				ingore = eventIgnore(materials);
			} catch (ClassCastException e) {
				return;
			}
			if (!ingore) {
				eventBean.setTarget(this.eventProcess(materials));
				eventBean.putEventProcessHandler(this);
			}
			// this.printAssemblyLine(eventBean);
		} catch (Exception e) {
			this.onException(e, eventBean, materials);
		} finally {
			// eventBeanMap.remove(sequence);
			eventBean.setSource(eventBean.getTarget());
		}
	}

	public void printAssemblyLine(EventProcesssBean eventBean) {
		if (eventBean.isEndOfBatch())
			logger.info("sequence:{} ==> {}  ", eventBean.getSequence(), StringUtils.join(eventBean.getAssemblyLine(), " ==> "));
	}

	@SuppressWarnings("unchecked")
	@Override
	public M refiningMaterials(Object source) throws ClassCastException {
		if (source == null)
			return null;
		M m = (M) source;
		return m;
	}

	@Override
	public void onException(Exception e, EventProcesssBean eventBean, M materials) {
		logger.error("生产线异常,sequence:{},lastHandler:{}", eventBean.getSequence(), eventBean.getPreviousHandler(), e);
	}

}
