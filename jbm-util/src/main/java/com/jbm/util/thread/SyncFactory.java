package com.jbm.util.thread;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;

public class SyncFactory {

	private static SyncFactory syncFactory = new SyncFactory();

	private Map<String, SyncBean> syncMap = Maps.newConcurrentMap();

	private AbstractScheduledService service = new AbstractScheduledService() {

		private AtomicBoolean runing = new AtomicBoolean(false);

		@Override
		protected void runOneIteration() throws Exception {
			if (runing.getAndSet(true))
				return;
			for (String key : syncMap.keySet()) {
				final SyncBean syncBean = syncMap.get(key);
				syncFactory.remove(syncBean);
			}
			runing.set(false);
		}

		@Override
		protected Scheduler scheduler() {
			return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.MINUTES);
		}
	};

	private SyncFactory() {
		super();
		service.startAsync();
	}

	public static SyncFactory getInstance() {
		return syncFactory;
	}

	public synchronized SyncBean createSyncBean(String name, int count) throws InterruptedException {
		SyncBean bean = new SyncBean(name, count, syncFactory);
		if (syncMap.containsKey(name)) {
			if (syncMap.get(name).getCount() > 0)
				throw new InterruptedException();
		}
		syncMap.put(name, bean);
		return bean;
	}

	public synchronized SyncBean createSyncBean(int count) throws InterruptedException {
		return createSyncBean(UUID.randomUUID().toString(), count);
	}

	public synchronized SyncBean createSyncBean() throws InterruptedException {
		return createSyncBean(1);
	}

	public void countDown(SyncBean syncBean) {
		if (syncBean == null)
			throw new NullPointerException();
		syncBean.countDown();
	}

	public void countDown(String name) {
		SyncBean syncBean = syncMap.get(name);
		countDown(syncBean);
	}

	public void remove(final SyncBean syncBean) {
		if (syncBean.getCount() <= 0) {
			synchronized (syncMap) {
				syncMap.remove(syncBean.getName());
			}
		}
	}
}
