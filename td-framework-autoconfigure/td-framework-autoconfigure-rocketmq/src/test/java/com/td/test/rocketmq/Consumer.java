package com.td.test.rocketmq;

import java.util.List;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;


public class Consumer {
	public static void main(String[] args) {
		DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("PushConsumer");
		consumer.setNamesrvAddr("192.168.14.58:9768");
		try {
			// 订阅PushTopic下Tag为push的消息
			consumer.subscribe("PushTopic", "push");
			// 程序第一次启动从消息队列头取数据
			consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
			consumer.registerMessageListener(new MessageListenerConcurrently() {
				public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext Context) {
					Message msg = list.get(0);
					System.out.println(msg.toString());
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}
			});
			consumer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
