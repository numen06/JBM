package com.td.framework.devops.actuator.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.MoreExecutors;

public class Test extends AbstractScheduledService {

	public Test() {
		// TODO Auto-generated constructor stub
	}

	public static Map<String, AbstractScheduledService> loop;

	static {
		loop = new HashMap<String, AbstractScheduledService>();
	}

	public static int i = 0;

	@Override
	protected void runOneIteration() throws Exception {
		// TODO Auto-generated method stub
		System.out.println(this.state());
		if (i == 4) {
			this.stopAsync();
			Test service = new Test();
			System.out.println("新的进程");
			service.addListener(new Listener() {
				@Override
				public void starting() {
					System.out.println("服务开始启动.....");
				}

				@Override
				public void running() {
					System.out.println("服务开始运行");
				}

				@Override
				public void stopping(State from) {
					System.out.println("服务关闭中");
				}

				@Override
				public void terminated(State from) {
					System.out.println("服务终止");
				}

				@Override
				public void failed(State from, Throwable failure) {
					System.out.println("失败，cause：" + failure.getCause());
				}
			}, MoreExecutors.directExecutor());
			service.startAsync().awaitRunning();
			i = 0;
		} else {
			i++;
		}

		// this.awaitTerminated();
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(1, 2, TimeUnit.SECONDS);
	}

	public static void main(String[] args) throws InterruptedException {
		Test service = new Test();

		// ServiceManager sm = new
		// ServiceManager(ListUtils.newArrayList(service, service2));

		service.addListener(new Listener() {
			@Override
			public void starting() {
				System.out.println("服务开始启动.....");
			}

			@Override
			public void running() {
				System.out.println("服务开始运行");
			}

			@Override
			public void stopping(State from) {
				System.out.println("服务关闭中");
			}

			@Override
			public void terminated(State from) {
				System.out.println("服务终止");
			}

			@Override
			public void failed(State from, Throwable failure) {
				System.out.println("失败，cause：" + failure.getCause());
			}
		}, MoreExecutors.directExecutor());

		service.startAsync().awaitRunning();
		// service2.startAsync().awaitRunning();

		// loop.put("1", service);
		// loop.put("2", service2);

		// loop.get("1").stopAsync().awaitTerminated();
		// loop.get("2").stopAsync().awaitTerminated();

		// System.out.println(sm.);

		// System.out.println("服务状态为:" + service.state());
		//
		// Thread.sleep(10 * 1000);
		//
		// service.stopAsync().awaitTerminated();
		//
		// System.out.println("服务状态为:" + service.state());
	}

}
