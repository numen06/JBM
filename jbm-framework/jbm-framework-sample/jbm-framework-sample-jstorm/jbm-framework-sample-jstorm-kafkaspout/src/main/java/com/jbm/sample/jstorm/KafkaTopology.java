package com.jbm.sample.jstorm;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.jbm.sample.jstorm.bolt.TestBolt;
import com.jbm.sample.jstorm.spout.TestSpout;

import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;

@SpringBootApplication
public class KafkaTopology {
	public static void main(String[] args) {
		SpringApplication.run(KafkaTopology.class, args);
	}

	@Autowired
	private TestSpout testSpout;

	@Autowired
	private TestBolt testBolt;
	
	@PostConstruct
	public void init() throws AlreadyAliveException, InvalidTopologyException {
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("testspout", testSpout, 1);
		builder.setBolt("testbolt", testBolt, 2).localOrShuffleGrouping("testspout");

		StormSubmitter.submitTopology("testtopology", System.getenv(), builder.createTopology());
		System.out.println("storm cluster will start");
	}

}