package com.td.sample.influx.kafka;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.influx.InfluxQueryParam;
import org.springframework.data.influx.InfluxTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;

@Service
public class InfluxService {

	@Autowired
	private InfluxTemplate influxTemplate;

	@PostConstruct
	public void init() {
		Map<String, Object> map = Maps.newHashMap();
		map.put("refValue", 1);
		map.put("psns", new String[] { "1484725420553121", "1484725420553121" });
		InfluxQueryParam influxQueryParam = new InfluxQueryParam("yd_data", map);
		influxQueryParam.getSupplementColumns().put("test", 1);
		System.out.println(JSON.toJSONString(influxTemplate.selectList("com.td.ev.program.mapper.EnenergysMapper.findBaseEnergyTypes", influxQueryParam)));
	}

	@Scheduled(cron = "*/21 * * * * ?")
	public void test() {
		init();
	}
}
