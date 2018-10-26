package com.jbm.framework.event;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.googlecode.gentyref.GenericTypeReflector;
import com.sohu.jafka.producer.Producer;
import com.sohu.jafka.producer.ProducerConfig;
import com.jbm.framework.event.bean.EventSendInfo;
import com.jbm.framework.event.bean.RemoteEventBean;
import com.jbm.framework.event.jafka.EventProducerData;
import com.jbm.framework.metadata.exceptions.ServiceException;
import com.jbm.util.CollectionUtils;

public class EventProducerService extends AbstractScheduledService {

	public static final Logger logger = LoggerFactory.getLogger(EventProducerService.class);

	private ListeningExecutorService eventSendExecutor;

	private final Queue<Collection<? extends ApplicationEvent>> messageQueue = new LinkedBlockingQueue<Collection<? extends ApplicationEvent>>();

	/**
	 * 派发者配置
	 */
	private final ProducerConfig producerConfigBean;

	private final String root;

	/**
	 * 派发者
	 */
	private Producer<String, RemoteEventBean> eventProducer;

	public EventProducerService(ProducerConfig producerConfigBean, String root) {
		super();
		this.producerConfigBean = producerConfigBean;
		this.root = root;
	}

	@Override
	protected void startUp() {
		this.eventSendExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
		eventProducer = new Producer<String, RemoteEventBean>(this.producerConfigBean);
	}

	private Boolean running = false;

	@Override
	protected void runOneIteration() throws Exception {
		if (messageQueue.isEmpty() && running == false) {
			return;
		}
		synchronized (running) {
			running = true;
			try {
				while (!messageQueue.isEmpty()) {
					final Collection<? extends ApplicationEvent> events = messageQueue.poll();
					eventSendExecutor.submit(new Callable<EventSendInfo>() {
						@Override
						public EventSendInfo call() throws Exception {
							EventProducerData eventProducerData = createEventSendData(events);
							eventProducer.send(eventProducerData);
							return new EventSendInfo(eventProducerData.getTopic(), new Date(), new Long(events.size()));
						}
					});
				}
			} finally {
				running = false;
			}
		}
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(0, 100, TimeUnit.MILLISECONDS);
	}

	public <E extends ApplicationEvent> void send(Collection<E> events) {
		this.messageQueue.add(events);
	}

	public <E extends ApplicationEvent> void send(E... events) {
		this.send(Lists.newArrayList(events));
	}

	private String buildNode(String group, String node) {
		return new StringBuffer(StringUtils.defaultIfBlank(group, root)).append("-").append(node).toString();
	}

	private <E extends ApplicationEvent> EventProducerData createEventSendData(Collection<E> events) throws ServiceException {
		ApplicationEvent tempEvent = CollectionUtils.firstNonEmptyResult(events);
		String node = GenericTypeReflector.getTypeName(tempEvent.getClass());
		String group = null;
		// 如果是远程时间支持分组
		if (tempEvent instanceof RemoteEventBean) {
			RemoteEventBean remoteEventBean = (RemoteEventBean) tempEvent;
			group = remoteEventBean.getGroup();
		}
		node = this.buildNode(group, node);
		return this.createEventSendData(node, events);
	}

	/**
	 * 集合成时间队列
	 * 
	 * @param node
	 * @param event
	 * @return
	 * @throws ServiceException
	 */
	private <E extends ApplicationEvent> EventProducerData createEventSendData(final String node, final Collection<E> events) throws ServiceException {
		final EventProducerData eventProducerData = new EventProducerData(node);
		// 设置相应的分片
		eventProducerData.setKey(node);
		for (Iterator<E> iterator = events.iterator(); iterator.hasNext();) {
			Object event = iterator.next();
			if (event == null)
				continue;
			if (event instanceof RemoteEventBean)
				eventProducerData.add((RemoteEventBean) event);
			else
				throw new ServiceException("事件不是RemoteEventBean的派生");
		}
		return eventProducerData;
	}

}
