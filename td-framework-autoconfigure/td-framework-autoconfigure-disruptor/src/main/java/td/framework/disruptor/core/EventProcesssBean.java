package td.framework.disruptor.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import td.framework.disruptor.handler.EventProcessHandler;

/**
 * 流水处理对象
 * 
 * @author wesley.zhang
 *
 */
public class EventProcesssBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4272291922853529736L;

	private long sequence;

	private Date createTime = new Date();

	private boolean endOfBatch;

	private Object source;

	private Object target;

	private EventProcessHandler<?, ?> previousHandler;

	@SuppressWarnings("rawtypes")
	private List<Class<? extends EventProcessHandler>> assemblyLine = new ArrayList<>();

	public EventProcesssBean() {
		super();
	}

	public EventProcesssBean(Object source) {
		super();
		this.source = source;
	}

	public long getSequence() {
		return sequence;
	}

	public void setSequence(long sequence) {
		this.sequence = sequence;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public boolean isEndOfBatch() {
		return endOfBatch;
	}

	public void setEndOfBatch(boolean endOfBatch) {
		this.endOfBatch = endOfBatch;
	}

	public Object getSource() {
		return source;
	}

	public void setSource(Object source) {
		this.source = source;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	@SuppressWarnings("rawtypes")
	public List<Class<? extends EventProcessHandler>> getAssemblyLine() {
		return assemblyLine;
	}

	// @SuppressWarnings("rawtypes")
	// public void setAssemblyLine(List<Class<? extends EventProcessHandler>>
	// assemblyLine) {
	// this.assemblyLine = assemblyLine;
	// }

	public void putEventProcessHandler(EventProcessHandler<?, ?> eventProcessHandler) {
		this.assemblyLine.add(eventProcessHandler.getClass());
		this.previousHandler = eventProcessHandler;
	}

	public EventProcessHandler<?, ?> getPreviousHandler() {
		return previousHandler;
	}

}
