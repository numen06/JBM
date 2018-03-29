package com.td.test.disruptor.consumer;

import com.alibaba.fastjson.JSON;
import com.lmax.disruptor.EventHandler;
import com.td.test.disruptor.bean.TradeTransaction;

/**
 * JMS消息消费者
 * 
 * @author wesley.zhang
 *
 */
public class TradeTransactionJMSNotifyHandler implements EventHandler<TradeTransaction> {

	@Override
	public void onEvent(TradeTransaction event, long sequence, boolean endOfBatch) throws Exception {
		event.setJsm(1);
		System.out.println("我是Jms：" + JSON.toJSONString(event));
	}
}
