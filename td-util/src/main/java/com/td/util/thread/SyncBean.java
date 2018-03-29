package com.td.util.thread;

import java.util.concurrent.CountDownLatch;

/**
 * 同步对象
 * 
 * @author wesley
 *
 */
public class SyncBean extends CountDownLatch {

	private final SyncFactory factory;

	/**
	 * 名字
	 */
	private final String name;

	public SyncBean(String name, Integer count, SyncFactory factory) {
		super(count);
		this.name = name;
		this.factory = factory;
	}

	public String getName() {
		return name;
	}

	@Override
	public void countDown() {
		synchronized (this) {
			super.countDown();
			factory.remove(this);
		}
	}

}
