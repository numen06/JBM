package com.td.framework.event;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ServiceManager;
import com.sohu.jafka.consumer.MessageStream;
import com.td.framework.event.bean.RemoteEventBean;
import com.td.util.ObjectUtils;

/**
 * 执行器服务
 * 
 * @author wesley
 *
 */
public class EventExecutorService extends AbstractExecutionThreadService {

	public static final Logger logger = LoggerFactory.getLogger(RemoteEventExecutor.class);

	// private ListeningExecutorService eventExecutor;
	private final List<MessageStream<RemoteEventBean>> streams;
	private final ApplicationContext applicationContext;
	private final EventBus eventBus;
	private final String node;
	private final Integer nThreads;
	private final EvictingQueue<RemoteEventBean> lastEventDeque = EvictingQueue.create(1);
	private ServiceManager streamsServiceManager;

	public EventExecutorService(List<MessageStream<RemoteEventBean>> streams, ApplicationContext applicationContext, EventBus eventBus, String node) {
		super();
		this.streams = streams;
		this.applicationContext = applicationContext;
		this.eventBus = eventBus;
		this.node = node;
		this.nThreads = 1;
	}

	public EventExecutorService(List<MessageStream<RemoteEventBean>> streams, ApplicationContext applicationContext, EventBus eventBus, String node, Integer nThreads) {
		super();
		this.streams = streams;
		this.applicationContext = applicationContext;
		this.eventBus = eventBus;
		this.node = node;
		this.nThreads = nThreads;
	}

	@Override
	protected void startUp() {
		// this.eventExecutor =
		// MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(nThreads));
		logger.info(MessageFormat.format("事件执行器开始运行-Node:{0},nTheads:", this.node, this.nThreads));
	}

	@Override
	protected void run() throws Exception {
		final AtomicInteger count = new AtomicInteger();
		List<EventMessageExecutor> esList = Lists.newArrayList();
		for (final MessageStream<RemoteEventBean> stream : streams) {
			EventMessageExecutor eme = new EventMessageExecutor(stream, count, applicationContext, eventBus, lastEventDeque);
			esList.add(eme);
		}
		streamsServiceManager = new ServiceManager(esList);
		streamsServiceManager.startAsync().awaitStopped();
	}

	@Override
	protected void triggerShutdown() {
		streamsServiceManager.stopAsync().awaitStopped();
	}

	public RemoteEventBean getLastEvent() {
		streamsServiceManager.awaitHealthy();
		return this.lastEventDeque.peek();
	}

}
