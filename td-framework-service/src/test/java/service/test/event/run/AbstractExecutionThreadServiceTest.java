package service.test.event.run;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;

public class AbstractExecutionThreadServiceTest extends AbstractExecutionThreadService {
	private volatile boolean running = true; // 声明一个状态

	@Override
	protected void startUp() {
		//// 在这里我们可以做一些初始化操作
		System.err.println("服务启动初始化");
	}

	@Override
	public void run() {
		while (running) {
			// do our work
			try {
				Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
				System.out.println("do our work.....");
			} catch (Exception e) {
				// 处理异常，这里如果抛出异常，会使服务状态变为failed同时导致任务终止。
			}
		}
	}

	@Override
	protected void triggerShutdown() {
		running = false; // 这里我们改变状态值，run方法中就能够得到响应。=
		// 可以做一些清理操作，也可以移到shutDown()方法中执行
	}

	@Override
	protected void shutDown() throws Exception {
		// 可以关闭资源，关闭连接。。。
		System.err.println("服务关闭");
	}

	public static void main(String[] args) throws InterruptedException {
		AbstractExecutionThreadServiceTest service = new AbstractExecutionThreadServiceTest();
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