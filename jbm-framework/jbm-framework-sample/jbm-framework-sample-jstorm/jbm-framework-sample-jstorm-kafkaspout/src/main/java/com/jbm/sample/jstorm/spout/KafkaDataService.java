package com.jbm.sample.jstorm.spout;

import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaDataService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1071231438031286369L;
	private final LinkedBlockingQueue<String> data = new LinkedBlockingQueue<>(1);

	public LinkedBlockingQueue<String> getData() {
		return data;
	}

	// private TestSpout testSpout;
	
	@KafkaListener(topics = "SMART1")
	public void processMessage(String content) {
		Thread thread = new Thread() {

			@Override
			public void run() {
				try {
					data.put(content);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		};
		thread.start();
	}

}
