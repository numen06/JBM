package com.jbm.framework.tools;

import java.util.concurrent.BlockingQueue;

import org.springframework.context.ApplicationContext;

import com.jbm.util.bean.queue.ForwardFilter;
import com.jbm.util.bean.queue.IForward;
import com.jbm.util.bean.queue.SortingQueue;

/**
 * 结合Spring的分拣队列，分拣器是由Spring进行管理的
 * 
 * @author wesley
 *
 * @param <K>
 * @param <E>
 */
public class SpringSortingQueue<K, E> extends SortingQueue<K, E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5715941181711578055L;
	private ApplicationContext applicationContext;

	public SpringSortingQueue(BlockingQueue<E> delegate, ForwardFilter<K, E> filter) {
		super(delegate, filter);
	}

	public SpringSortingQueue(BlockingQueue<E> delegate, ForwardFilter<K, E> filter, ApplicationContext applicationContext) {
		super(delegate, filter);
		this.applicationContext = applicationContext;
	}

	public <T extends IForward<K, E>> void addForward(Class<T> bean) throws Exception {
		applicationContext.getAutowireCapableBeanFactory().createBean(bean);
	}
}
