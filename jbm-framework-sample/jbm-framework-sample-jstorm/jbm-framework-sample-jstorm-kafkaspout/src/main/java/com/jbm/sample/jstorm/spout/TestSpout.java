package com.jbm.sample.jstorm.spout;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

@Service
public class TestSpout extends BaseRichSpout {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8589894046570332916L;
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSpout.class);
	static AtomicInteger sAtomicInteger = new AtomicInteger(0);
	static AtomicInteger pendNum = new AtomicInteger(0);
	private int sqnum;
	private SpoutOutputCollector collector;

	@Autowired
	private KafkaDataService kafkaDataService;

	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		sqnum = sAtomicInteger.incrementAndGet();
		this.collector = collector;
	}

	private LinkedBlockingQueue<String> data = new LinkedBlockingQueue<>(1);

	@Override
	public void nextTuple() {
		int a = pendNum.incrementAndGet();
		try {
			LOGGER.info(String.format("spount %d,pendNum %d", sqnum, a));
			this.collector.emit(new Values(kafkaDataService.getData().take()));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("log"));
	}

}