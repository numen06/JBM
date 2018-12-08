package jbm.framework.boot.autoconfigure.canal.event;

import org.springframework.context.ApplicationEvent;

import com.alibaba.otter.canal.protocol.CanalEntry.Entry;

/**
 * @author <a href="mailto:wangchao.star@gmail.com">wangchao</a>
 * @version 1.0
 * @since 2017-08-26 22:22:00
 */
public abstract class CanalEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4225923094962385316L;

	/**
	 * Create a new ApplicationEvent.
	 *
	 * @param source the object on which the event initially occurred (never
	 *               {@code null})
	 */
	public CanalEvent(Entry source) {
		super(source);
	}

	public Entry getEntry() {
		return (Entry) source;
	}
}
