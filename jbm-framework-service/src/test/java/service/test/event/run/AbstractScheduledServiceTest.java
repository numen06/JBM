package service.test.event.run;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * User: hupeng Date: 14-12-22 Time: 下午7:43
 */
public class AbstractScheduledServiceTest extends AbstractScheduledService {
	@Override
	protected void startUp() throws Exception {
		System.err.println("服务启动初始化");
	}

	@Override
	protected void shutDown() throws Exception {
		System.err.println("服务关闭");
	}

	@Override
	protected void runOneIteration() throws Exception {
		// //处理异常，这里如果抛出异常，会使服务状态变为failed同时导致任务终止
		try {
			System.out.println("do work....");
		} catch (Exception e) {
			// 处理异常
		}
	}

	@Override
	protected Scheduler scheduler() {
		// 多少周期执行一次
		return Scheduler.newFixedDelaySchedule(0, 2, TimeUnit.SECONDS);
	}

	public static void main(String[] args) throws InterruptedException {
		AbstractScheduledServiceTest service = new AbstractScheduledServiceTest();
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
		System.out.println("服务状态为:" + service.state());
		Thread.sleep(10 * 1000);
		service.stopAsync().awaitTerminated();
		System.out.println("服务状态为:" + service.state());
	}
}