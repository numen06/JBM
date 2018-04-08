package service.test.event.remote;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableMap;
import com.sohu.jafka.consumer.Consumer;
import com.sohu.jafka.consumer.ConsumerConfig;
import com.sohu.jafka.consumer.ConsumerConnector;
import com.sohu.jafka.consumer.MessageStream;
import com.sohu.jafka.producer.Producer;
import com.sohu.jafka.producer.ProducerConfig;
import com.sohu.jafka.producer.StringProducerData;
import com.sohu.jafka.producer.serializer.StringDecoder;
import com.sohu.jafka.producer.serializer.StringEncoder;

public class JafkaTest {
	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
		props.put("zk.connect", "192.168.1.6:2181");
		props.put("serializer.class", StringEncoder.class.getName());
		//
		ProducerConfig config = new ProducerConfig(props);
		Producer<String, String> producer = new Producer<String, String>(config);
		//
		StringProducerData data = new StringProducerData("demo");
		for (int i = 0; i < 100; i++) {
			data.add("Hello world #" + i);
		}
		//
		try {
			long start = System.currentTimeMillis();
			for (int i = 0; i < 100; i++) {
				producer.send(data);
			}
			long cost = System.currentTimeMillis() - start;
			System.out.println("send 10000 message cost: " + cost + " ms");
		} finally {
			producer.close();
		}
		props.put("groupid", "test_group");
		//
		ConsumerConfig consumerConfig = new ConsumerConfig(props);
		ConsumerConnector connector = Consumer.create(consumerConfig);
		//
		Map<String, List<MessageStream<String>>> topicMessageStreams = connector.createMessageStreams(ImmutableMap.of("demo", 2), new StringDecoder());
		List<MessageStream<String>> streams = topicMessageStreams.get("demo");
		//
		ExecutorService executor = Executors.newFixedThreadPool(2);
		final AtomicInteger count = new AtomicInteger();
		for (final MessageStream<String> stream : streams) {
			executor.submit(new Runnable() {

				public void run() {
					for (String message : stream) {
						System.out.println(count.incrementAndGet() + " => " + message);
					}
				}
			});
		}
		//
		executor.awaitTermination(1, TimeUnit.HOURS);
	}

}
