package jbm.framework.boot.autoconfigure.canal.event;

import com.alibaba.otter.canal.protocol.CanalEntry.Entry;

/**
 * @author <a href="mailto:wangchao.star@gmail.com">wangchao</a>
 * @version 1.0
 * @since 2017-08-26 22:30:00
 */
public class InsertCanalEvent extends CanalEvent {
    /**
     *
     */
    private static final long serialVersionUID = 5978628196901969939L;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public InsertCanalEvent(Entry source) {
        super(source);
    }
}
