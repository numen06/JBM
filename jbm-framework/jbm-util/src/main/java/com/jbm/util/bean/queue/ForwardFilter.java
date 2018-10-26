package com.jbm.util.bean.queue;

/**
 * 过滤器
 * 
 * @author wesley
 *
 * @param <T>
 */
public interface ForwardFilter<K, E> {

	/**
	 * -1:直接返回;0:丢弃;>1:转发
	 * 
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	ForwardKey<K> filter(final E obj) throws Exception;

}
