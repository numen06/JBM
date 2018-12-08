package com.jbm.test.disruptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import com.jbm.test.disruptor.bean.TradeTransaction;
import com.jbm.test.disruptor.consumer.TradeTransactionInDBHandler;
import com.jbm.test.disruptor.consumer.TradeTransactionJMSNotifyHandler;
import com.jbm.test.disruptor.consumer.TradeTransactionVasConsumer;
import com.jbm.test.disruptor.translator.TradeTransactionEventTranslator;
import com.jbm.test.disruptor.translator.TradeTransactionPublisher;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;

import jbm.framework.disruptor.core.DisruptorTemplate;

public class DisruptorTest {
	@SuppressWarnings("unchecked")
	@Test
	public void testMore() throws InterruptedException {
		long beginTime = System.currentTimeMillis();

		int bufferSize = 1024;

		ExecutorService executor = Executors.newFixedThreadPool(4);

		ThreadPoolExecutorFactoryBean pool = new ThreadPoolExecutorFactoryBean();
		pool.setCorePoolSize(10);

		Disruptor<TradeTransaction> disruptor = new Disruptor<TradeTransaction>(new EventFactory<TradeTransaction>() {
			@Override
			public TradeTransaction newInstance() {
				return new TradeTransaction();
			}
		}, bufferSize, pool);

		EventHandlerGroup<TradeTransaction> handlerGroup = disruptor.handleEventsWith(new TradeTransactionInDBHandler(),
				new TradeTransactionVasConsumer());

		TradeTransactionJMSNotifyHandler jmsConsumer = new TradeTransactionJMSNotifyHandler();
		// 声明在C1,C2完事之后执行JMS消息发送操作 也就是流程走到C3
		handlerGroup.then(jmsConsumer);

		disruptor.start();
		CountDownLatch latch = new CountDownLatch(1);
		executor.submit(new TradeTransactionPublisher(latch, disruptor));
		latch.await();// 等待生产者完事.
		disruptor.shutdown();
		executor.shutdown();

		System.out.println("总耗时:" + (System.currentTimeMillis() - beginTime));
	}

	@Test
	public void testOne() throws TimeoutException, InterruptedException {
		long beginTime = System.currentTimeMillis();
		DisruptorTemplate<TradeTransaction> disruptorTemplate = new DisruptorTemplate<TradeTransaction>(
				new EventFactory<TradeTransaction>() {

					@Override
					public TradeTransaction newInstance() {
						return new TradeTransaction();
					}
				});
//		EventHandlerGroup group = disruptorTemplate.getDisruptor().handleEventsWith(new TradeTransactionVasConsumer());
		disruptorTemplate.getDisruptor().start();
		TradeTransactionEventTranslator tradeTransloator = new TradeTransactionEventTranslator();
		System.out.println("----start----");
		disruptorTemplate.getDisruptor().publishEvent(tradeTransloator);
		disruptorTemplate.getDisruptor().publishEvent(tradeTransloator);
		disruptorTemplate.getDisruptor().publishEvent(tradeTransloator);
		disruptorTemplate.getDisruptor().publishEvent(tradeTransloator);
		System.out.println("----send----");
		disruptorTemplate.getDisruptor().shutdown();
		// group.then(new TradeTransactionInDBHandler());
		disruptorTemplate.getDisruptor().publishEvent(tradeTransloator);
		System.out.println("指针：" + disruptorTemplate.getDisruptor().getCursor());
		System.out.println("总耗时:" + (System.currentTimeMillis() - beginTime));
	}
}
