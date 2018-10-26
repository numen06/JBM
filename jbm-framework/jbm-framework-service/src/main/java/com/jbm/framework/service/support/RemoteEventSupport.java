package com.jbm.framework.service.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ResourceUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.MoreExecutors;
import com.googlecode.gentyref.GenericTypeReflector;
import com.sohu.jafka.consumer.ConsumerConfig;
import com.sohu.jafka.producer.ProducerConfig;
import com.sohu.jafka.utils.Utils;
import com.jbm.framework.event.EventProducerService;
import com.jbm.framework.event.RemoteEventExecutor;
import com.jbm.framework.event.bean.IEventListener;
import com.jbm.framework.event.bean.RemoteEventBean;
import com.jbm.framework.metadata.exceptions.ServiceException;
import com.jbm.framework.utils.ProxyUtils;
import com.jbm.util.AnnotatedUtils;
import com.jbm.util.bean.MethodWrap;

/**
 * 远程事件支持
 * 
 * @author wesley
 *
 */
public class RemoteEventSupport implements ApplicationContextAware {

	public static final Logger logger = LoggerFactory.getLogger(RemoteEventSupport.class);

	public static final String ROOT = "zooKeeperEvent";

	/**
	 * 事件公车
	 */
	private EventBus eventBus;

	/**
	 * 监听事件的节点，可以区分不同平台
	 */
	private String eventNode = ROOT;

	/**
	 * 如果启用异步执行器发生时间较长的事件无法完全执行，如果中途中断会失去事件
	 */
	private Boolean asyncEvent = false;

	public String getEventNode() {
		return eventNode;
	}

	private RemoteEventExecutor remoteEventExecutor;

	/**
	 * Spring容器
	 */
	private ApplicationContext applicationContext;

	public RemoteEventSupport() {
		super();
	}

	public RemoteEventSupport(String eventNode) {
		super();
		this.eventNode = eventNode;
	}

	public RemoteEventSupport(String eventNode, Boolean asyncEvent) {
		super();
		this.eventNode = eventNode;
		this.asyncEvent = asyncEvent;
	}

	public void setAsyncEvent(Boolean asyncEvent) {
		this.asyncEvent = asyncEvent;
	}

	private String producerConfig;
	private String consumerConfig;
	private ConsumerConfig consumerConfigBean;
	private ProducerConfig producerConfigBean;

	public RemoteEventSupport(ConsumerConfig consumerConfigBean, ProducerConfig producerConfigBean) {
		super();
		this.consumerConfigBean = consumerConfigBean;
		this.producerConfigBean = producerConfigBean;
	}

	private EventProducerService eventProducerService;

	public void setEventNode(String eventNode) {
		this.eventNode = eventNode;
	}

	public void setProducerConfig(String producerConfig) {
		this.producerConfig = producerConfig;
	}

	public void setConsumerConfig(String consumerConfig) {
		this.consumerConfig = consumerConfig;
	}

	private boolean init = false;

	@PostConstruct
	public void init() throws IOException {
		if (init)
			return;
		// 先初始化事件总线
		this.eventBus = new AsyncEventBus(this.eventNode, asyncEvent ? Executors.newCachedThreadPool() : MoreExecutors.directExecutor());
		// 初始化远程事件总线
		initRemote();
		// 自动注入监听
		autoRegisterApplicationListener();
		// 自动注册监听
		autoRegisterEventListener();
		// 创建监听
		this.listener(this.eventNode);
		// 监听开始执行
		this.remoteEventExecutor.run();
		init = true;
	}

	public void initRemote() throws IOException {
		if (StringUtils.isNotBlank(this.producerConfig)) {
			Properties props = PropertiesLoaderUtils.loadProperties(applicationContext.getResources(this.producerConfig)[0]);
			this.producerConfigBean = new ProducerConfig(props);
			// eventProducer = new Producer<String,
			// RemoteEventBean>(this.producerConfigBean);
			this.eventProducerService = new EventProducerService(producerConfigBean, this.eventNode);
			eventProducerService.startAsync();
		}

		if (StringUtils.isNotBlank(this.consumerConfig)) {
			Properties props = PropertiesLoaderUtils.loadProperties(applicationContext.getResources(this.consumerConfig)[0]);
			// this.config = new ConsumerConfig(props);
			this.consumerConfigBean = new ConsumerConfig(props);
			// eventConsumerConnector =
			// Consumer.create(this.consumerConfigBean);
		}
	}

	/**
	 * 自动载入IEventListener
	 */
	private void autoRegisterEventListener() {
		Map<String, IEventListener> liss = applicationContext.getBeansOfType(IEventListener.class);
		for (IEventListener proxy : liss.values()) {
			Object obj = null;
			try {
				obj = ProxyUtils.getTarget(proxy);
			} catch (Exception e) {
				logger.error("处理代理类出错", e);
			}
			if (obj == null)
				continue;
			this.registerEventListener(obj);
		}
	}

	/**
	 * Spring自动载入环境中的自动注入
	 */
	private void autoRegisterApplicationListener() {
		String[] lis = applicationContext.getBeanNamesForType(ApplicationListener.class);
		for (int i = 0; i < lis.length; i++) {
			Class<?> clazz = applicationContext.getType(lis[i]);
			try {
				Type type = GenericTypeReflector.getTypeParameter(clazz, ApplicationListener.class.getTypeParameters()[0]);
				if (type instanceof ParameterizedType) {
					ParameterizedType p_type = (ParameterizedType) type;
					String event = GenericTypeReflector.getTypeName(p_type.getRawType());
					this.listener(event);
				} else {
					String event = GenericTypeReflector.getTypeName(type);
					this.listener(event);
				}
			} catch (Exception e) {
				continue;
			}
		}
	}

	/**
	 * 抛出一个事件
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public <E extends ApplicationEvent> boolean postEvent(Collection<E> events) throws Exception {
		return this.postEvent(this.eventNode, events);
	}

	/**
	 * 抛出一个事件
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public <E extends ApplicationEvent> boolean postEvent(E... events) throws Exception {
		return this.postEvent(this.eventNode, ImmutableList.copyOf(events));
	}

	/**
	 * 发送一个本地事件
	 * 
	 * @param event
	 * @return
	 * @throws ServiceException
	 */
	public <E extends ApplicationEvent> boolean postLocalEvent(E... event) throws ServiceException {
		return postLocalEvent(ImmutableList.copyOf(event));
	}

	/**
	 * 发送本地时间不通过网络传输
	 * 
	 * @param event
	 * @return
	 * @throws ServiceException
	 */
	public <E extends ApplicationEvent> boolean postLocalEvent(Collection<E> events) throws ServiceException {
		for (Iterator<E> iterator = events.iterator(); iterator.hasNext();) {
			E event = iterator.next();
			if (event == null)
				throw new ServiceException("发送事件为空");
			applicationContext.publishEvent(event);
			eventBus.post(event);
		}
		return true;
	}

	/**
	 * 提交事件
	 * 
	 * @param node
	 *            节点名:/xxx形式
	 * @param event
	 *            需要提交的事件
	 * @return
	 * @throws Exception
	 */
	protected <E extends ApplicationEvent> boolean postEvent(String root, Collection<E> events) throws Exception {
		boolean result = false;
		try {
			if (producerConfig == null) {
				this.postLocalEvent(events);
				return true;
			}
			eventProducerService.send(events);
			result = true;
		} catch (Exception e) {
			logger.error("派发远程事件错误", e);
		}
		return result;
	}

	/**
	 * 
	 * 注册监听类
	 * 
	 * @param listener
	 */
	public <T extends IEventListener> boolean listener(T listener) {
		return this.registerEventListener(listener);
	}

	/**
	 * 注册相应的监听类
	 * 
	 * @param bean
	 * @return
	 */
	protected boolean registerEventListener(Object bean) {
		Multimap<Class<?>, MethodWrap> parameters = AnnotatedUtils.findAllParameters(bean, Subscribe.class);
		if (parameters == null || parameters.isEmpty())
			return false;
		for (Class<?> key : parameters.keys()) {
			String event = GenericTypeReflector.getTypeName(key);
			this.listener(event);
		}
		this.eventBus.register(bean);
		return true;
	}

	/**
	 * 
	 * 获取最后一条消息
	 * 
	 * @param eventClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends RemoteEventBean> T getLastMessage(Class<T> eventClass) {
		String eventType = GenericTypeReflector.getTypeName(eventClass);
		return (T) this.remoteEventExecutor.getLastMessage(eventType);
	}

	/**
	 * 注册远程监听
	 * 
	 * @param node
	 *            节点名称
	 * @return
	 */
	protected boolean listener(String node) {
		try {
			if (this.consumerConfig != null) {
				// 如果不存在，则加入列表
				if (remoteEventExecutor == null) {
					this.remoteEventExecutor = new RemoteEventExecutor(this.consumerConfigBean, this.applicationContext, this.eventBus, this.getEventNode());
				}
				this.remoteEventExecutor.pushNode(node);
			}
			logger.debug(MessageFormat.format("add listener node:{0}", node));
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
