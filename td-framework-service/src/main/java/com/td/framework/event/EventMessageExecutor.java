package com.td.framework.event;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.EvictingQueue;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.sohu.jafka.consumer.MessageStream;
import com.td.framework.event.bean.RemoteEventBean;

/**
 * 每条数据执行器
 * 
 * @author wesley
 *
 */
public class EventMessageExecutor extends AbstractExecutionThreadService {
	public static final Logger logger = LoggerFactory.getLogger(EventMessageExecutor.class);

	private final MessageStream<RemoteEventBean> stream;
	private final AtomicInteger count;
	private final ApplicationContext applicationContext;
	private final EventBus eventBus;
	private final EvictingQueue<RemoteEventBean> lastEventDeque;

	public EventMessageExecutor(MessageStream<RemoteEventBean> stream, AtomicInteger count, ApplicationContext applicationContext, EventBus eventBus,
		EvictingQueue<RemoteEventBean> lastEventDeque) {
		super();
		this.stream = stream;
		this.count = count;
		this.applicationContext = applicationContext;
		this.eventBus = eventBus;
		this.lastEventDeque = lastEventDeque;
	}

	@Override
	protected void run() throws Exception {
		for (final RemoteEventBean message : stream) {
			if (message == null) {
				logger.error(MessageFormat.format("远程传输时间错误-MessageSn:{0}", count.incrementAndGet()));
				continue;
			}
			lastEventDeque.offer(message);
			applicationContext.publishEvent(message);
			eventBus.post(message);
		}
	}

}
