package com.td.framework.event;

import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.SerializationUtils;

import com.google.common.eventbus.EventBus;
import com.td.framework.event.bean.RemoteEventBean;

/**
 * 远程事件监听
 * 
 * @author wesley
 *
 */
public class RemoteEventWatch implements NodeCacheListener {

	public static final Logger logger = LoggerFactory.getLogger(RemoteEventWatch.class);

	private final ApplicationContext applicationContext;
	private final EventBus eventBus;
	private final NodeCache node;

	public RemoteEventWatch(NodeCache node, ApplicationContext applicationContext, EventBus eventBus) {
		super();
		this.node = node;
		this.eventBus = eventBus;
		this.applicationContext = applicationContext;
	}

	/**
	 * 当节点发生变化的时候时候触发事件处理
	 */
	@Override
	public void nodeChanged() throws Exception {
		if (node.getCurrentData() == null)
			return;
		byte[] bytes = node.getCurrentData().getData();
		if (bytes != null) {
			RemoteEventBean bean = null;
			try {
				bean = (RemoteEventBean) SerializationUtils.deserialize(bytes);
				if (bean.getSource() == null) {
					bean.setSource(node);
				}
				applicationContext.publishEvent(bean);
				eventBus.post(bean);
			} catch (Exception e) {
				logger.error("节点切换错误", e);
			}
		}
	}
}
