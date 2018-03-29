package com.td.util.bean.queue;

import java.util.concurrent.BlockingQueue;

/**
 * 虚拟转发器
 * 
 * @author wesley
 *
 * @param <K>
 * @param <E>
 */
public abstract class AbstractForward<K, E> implements IForward<K, E> {

	private K key;
	private BlockingQueue<E> forwordQueue;
	private ForwardState forwardState = ForwardState.RUNING;

	public K getKey() {
		return key;
	}

	public void setKey(K key) {
		this.key = key;
	}

	public BlockingQueue<E> getForwordQueue() {
		return forwordQueue;
	}

	public void setForwordQueue(BlockingQueue<E> forwordQueue) {
		this.forwordQueue = forwordQueue;
	}

	@Override
	public void initForward(K key, BlockingQueue<E> forwordQueue) {
		this.key = key;
		this.forwordQueue = forwordQueue;
	}

	@Override
	public void forword(E element, BlockingQueue<E> forwordQueue) {

	}

	@Override
	public ForwardState getForwardState() {
		return this.forwardState;
	}

	public void setForwardState(ForwardState forwardState) {
		this.forwardState = forwardState;
	}

	@Override
	public void timeout(int time) {
	}

}
