package com.jbm.util.bean.queue;

import java.util.concurrent.BlockingQueue;

/**
 * 转发处理器
 * 
 * @author wesley
 *
 * @param <K>
 * @param <E>
 */
public interface IForward<K, E> {

	/**
	 * 当前转发器的主键
	 * 
	 * @return
	 */
	public K getKey();

	/**
	 * 创建队列
	 * 
	 * @param key
	 *            主键
	 * @param forwordQueue
	 *            创建队列
	 */
	public ForwardState getForwardState();

	/**
	 * 转发
	 * 
	 * @param element
	 *            转发对象
	 * @param forwordQueue
	 *            转发队列
	 */
	public void forword(E element, BlockingQueue<E> forwordQueue);

	/**
	 * 主队列超时
	 * 
	 * @param time
	 *            超时次数
	 */
	public void timeout(int time);

	/**
	 * 初始化
	 * 
	 * @param key
	 * @param forwordQueue
	 */
	public void initForward(K key, BlockingQueue<E> forwordQueue);

}
