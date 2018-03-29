package com.td.util.bean.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ForwardingBlockingQueue;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.td.util.MapUtils;

/**
 * 
 * 分拣队列:对放进队列的数据进行过滤和分拣
 * 
 * @author Wesley
 *
 * @param <E>
 */
public class SortingQueue<K, E> extends ForwardingBlockingQueue<E>implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final BlockingQueue<E> delegate;
	private final AtomicInteger time = new AtomicInteger(0);
	private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
	private final AtomicInteger timeout = new AtomicInteger(60);
	private final Queue<IForward<K, E>> spareForward = Queues.newLinkedBlockingDeque();
	private Callable<IForward<K, E>> callForward;

	private ForwardFilter<K, E> filter = new ForwardFilter<K, E>() {
		@Override
		public ForwardKey<K> filter(final E t) {
			return new ForwardKey<K>(ForwardType.NORMAL, null);
		}
	};
	private final Map<K, BlockingQueue<E>> forwardingQueues = Maps.newConcurrentMap();
	private final Map<K, IForward<K, E>> forwards = Maps.newConcurrentMap();

	protected final Service loopService = new AbstractExecutionThreadService() {
		@Override
		protected void run() throws Exception {
			while (this.isRunning()) {
				try {
					final E element = delegate.poll(timeout.get(), TimeUnit.SECONDS);
					doFilter(element);
				} catch (InterruptedException e) {
					time.getAndAdd(1);
					e.printStackTrace();
				}
			}
		}
	};

	protected final Service forwardService = new AbstractScheduledService() {

		@Override
		protected void runOneIteration() throws Exception {
			synchronized (forwards) {
				List<IForward<K, E>> _list = new ArrayList<IForward<K, E>>();
				for (K k : forwards.keySet()) {
					IForward<K, E> f = forwards.get(k);
					if (f.getForwardState().equals(ForwardState.DESTROY)) {
						_list.add(f);
					}
				}
				for (Iterator<IForward<K, E>> iterator = _list.iterator(); iterator.hasNext();) {
					IForward<K, E> iForward = iterator.next();
					forwards.remove(iForward);
				}
			}
		}

		@Override
		protected Scheduler scheduler() {
			return Scheduler.newFixedRateSchedule(0, 5, TimeUnit.SECONDS);
		}

	};

	private void doFilter(final E element) {
		final ListenableFuture<Map<K, E>> listenableFuture = executorService.submit(new Callable<Map<K, E>>() {
			@Override
			public Map<K, E> call() throws Exception {
				final ForwardKey<K> key = filter.filter(element);
				if (key == null || key.getKey() == null)
					return null;
				if (key.getForwardType() == ForwardType.NORMAL) {
					delegate.offer(element);
					return null;
				}
				if (key.getForwardType() == ForwardType.RECOVER) {
					return null;
				}
				findForward(key.getKey());
				return ImmutableMap.of(key.getKey(), element);
			}
		});
		Futures.addCallback(listenableFuture, new FutureCallback<Map<K, E>>() {
			@Override
			public void onSuccess(Map<K, E> result) {
				if (result == null)
					return;
				for (final K key : result.keySet()) {
					final E val = result.get(key);
					if (MapUtils.isEmpty(result))
						return;
					final BlockingQueue<E> forwordQueue = forwardingQueues.get(key);
					forwordQueue.offer(val);
					final IForward<K, E> forword = findForward(key, forwordQueue);
					if (forword != null)
						forword.forword(val, forwordQueue);
				}
			}

			@Override
			public void onFailure(Throwable t) {
				t.printStackTrace();
			}
		});
	}

	protected final IForward<K, E> findForward(K key) {
		BlockingQueue<E> forwordQueue = Queues.newLinkedBlockingQueue();
		synchronized (forwardingQueues) {
			if (forwardingQueues.containsKey(key)) {
				forwordQueue = forwardingQueues.get(key);
			} else {
				forwardingQueues.put(key, forwordQueue);
			}
		}
		return findForward(key, forwordQueue);
	}

	protected final IForward<K, E> findForward(K key, BlockingQueue<E> forwordQueue) {
		synchronized (forwards) {
			IForward<K, E> froward = forwards.get(key);
			if (froward != null)
				return froward;
			froward = spareForward.poll();
			if (froward != null) {
				// 初始化
				froward.initForward(key, forwordQueue);
				forwards.put(key, froward);
				return froward;
			}
			try {
				if (callForward == null)
					return null;
				froward = callForward.call();
				forwards.put(key, froward);
				// 初始化
				froward.initForward(key, forwordQueue);
				return froward;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public SortingQueue(BlockingQueue<E> delegate, ForwardFilter<K, E> filter) {
		super();
		this.delegate = delegate;
		this.filter = filter;
		forwardService.startAsync();
	}

	protected void executorTimeout() {
		for (K k : forwards.keySet()) {
			IForward<K, E> f = forwards.get(k);
			f.timeout(time.get());
		}
	}

	// protected void executorCreate(K key, E val, BlockingQueue<E> queue) {
	// for (K k : forwards.keySet()) {
	// IForward<K, E> f = forwards.get(k);
	// f.create(key, queue);
	// }
	// }
	//
	// protected void executorDestroy(K key, E val, BlockingQueue<E> queue) {
	// for (K k : forwards.keySet()) {
	// IForward<K, E> f = forwards.get(k);
	// f.destroy(key, queue);
	// }
	// }

	@Override
	protected BlockingQueue<E> delegate() {
		return this.delegate;
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		return standardAddAll(collection);
	}

	@Override
	public boolean contains(Object object) {
		return delegate().contains(checkNotNull(object));
	}

	@Override
	public boolean remove(Object object) {
		return delegate().remove(checkNotNull(object));
	}

	public void addForward(IForward<K, E> forword) {
		final K k = forword.getKey();
		if (k == null)
			this.spareForward.offer(forword);
		else
			this.forwards.put(k, forword);
	}

	public void addForward(Callable<IForward<K, E>> callback) throws Exception {
		callForward = callback;
		callForward.call();
	}

	public void sorting() {
		loopService.startAsync().awaitRunning();
	}

	public void stop() {
		loopService.stopAsync().awaitTerminated();
	}

	public void awaitTerminated() {
		loopService.awaitTerminated();
	}

	public void setTimeout(Integer timeout) {
		this.timeout.set(timeout);
	}

}
