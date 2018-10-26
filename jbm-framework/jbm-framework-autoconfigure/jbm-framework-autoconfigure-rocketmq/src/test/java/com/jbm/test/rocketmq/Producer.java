package com.jbm.test.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

public class Producer {
	public static void main(String[] args) {
		DefaultMQProducer producer = new DefaultMQProducer("Producer");
		producer.setNamesrvAddr("192.168.14.58:9876");
		try {
			producer.start();
//			producer.createTopic(producer.getCreateTopicKey(), "PushTopic", 1);
			Message msg = new Message("PushTopic", "push", "1", "Just for test.".getBytes());
			SendResult result = producer.send(msg);
			System.out.println("id:" + result.getMsgId() + " result:" + result.getSendStatus());
			msg = new Message("PushTopic", "push", "2", "Just for test.".getBytes());
			result = producer.send(msg);
			System.out.println("id:" + result.getMsgId() + " result:" + result.getSendStatus());
			msg = new Message("PullTopic", "pull", "1", "Just for test.".getBytes());
			result = producer.send(msg);
			System.out.println("id:" + result.getMsgId() + " result:" + result.getSendStatus());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			producer.shutdown();
		}
	}
}
