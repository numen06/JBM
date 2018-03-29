package com.td.framework.event;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.sohu.jafka.consumer.Consumer;
import com.sohu.jafka.consumer.ConsumerConfig;
import com.sohu.jafka.consumer.ConsumerConnector;
import com.sohu.jafka.consumer.MessageStream;
import com.td.framework.event.bean.RemoteEventBean;
import com.td.framework.event.jafka.EventDecoder;
import com.td.util.StringUtils;

/**
 * 事件执行者
 * 
 * @author wesley
 *
 */
public class RemoteEventExecutor implements Runnable {
	public static final Logger logger = LoggerFactory.getLogger(RemoteEventExecutor.class);

	/**
	 * 执行者
	 */
	private ConsumerConnector eventConsumerConnector;
	private final ConsumerConfig consumerConfigBean;
	private final ApplicationContext applicationContext;
	private final EventBus eventBus;
	private final String root;
	private final Map<String, Integer> nodes = Maps.newConcurrentMap();
	private ServiceManager serviceManager = null;
	private final Map<String, EventExecutorService> eventExecutorMap = Maps.newConcurrentMap();
	private final EventDecoder eventDecoder = new EventDecoder();
	private final LoadingCache<String, RemoteEventBean> lastEventCache = CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<String, RemoteEventBean>() {
		@Override
		public RemoteEventBean load(String key) throws Exception {
			final EventExecutorService ex = eventExecutorMap.get(key);
			serviceManager.awaitHealthy();
			return ex.getLastEvent();
		}
	});

	public RemoteEventExecutor(ConsumerConfig consumerConfigBean, ApplicationContext applicationContext, EventBus eventBus, String root) {
		super();
		this.consumerConfigBean = consumerConfigBean;
		this.applicationContext = applicationContext;
		this.eventBus = eventBus;
		this.root = root;
	}

	private Map<String, List<MessageStream<RemoteEventBean>>> topicMessageStreams = null;

	private String buildNode(String node) {
		return new StringBuffer(root).append("-").append(node).toString();
	}

	public void pushNode(String node) {
		this.pushNode(node, 2);
	}

	public void pushNode(String node, Integer threads) {
		String tempNode = this.buildNode(node);
		if (this.nodes.containsKey(tempNode))
			return;
		this.nodes.put(this.buildNode(node), threads);
		if (serviceManager != null)
			this.restart();
	}

	/**
	 * 获取最后一条消息
	 * 
	 * @param node
	 * @return
	 */
	public RemoteEventBean getLastMessage(String node) {
		String tempNode = this.buildNode(node);
		if (StringUtils.isBlank(tempNode))
			return null;
		while (state == 0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			return this.lastEventCache.get(tempNode);
		} catch (Exception e) {
			return null;
		}
	}

	public ServiceManager getServiceManager() {
		return serviceManager;
	}

	private int state = 0;

	@Override
	public void run() {
		if (state == 0)
			eventConsumerConnector = Consumer.create(this.consumerConfigBean);
		topicMessageStreams = eventConsumerConnector.createMessageStreams(nodes, eventDecoder);
		List<Service> services = Lists.newArrayList();
		for (String node : nodes.keySet()) {
			EventExecutorService ex = new EventExecutorService(topicMessageStreams.get(node), applicationContext, eventBus, node);
			services.add(ex);
			eventExecutorMap.put(node, ex);
		}
		serviceManager = new ServiceManager(services);
		serviceManager.startAsync();
		state = 1;
	}

	public void restart() {
		this.stop();
		this.run();
	}

	public void stop() {
		try {
			eventConsumerConnector.close();
			serviceManager.stopAsync().awaitStopped(1, TimeUnit.SECONDS);
			serviceManager.awaitStopped();
			eventExecutorMap.clear();
			serviceManager = null;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		state = 0;
	}

}
