package com.td.sample.influx;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.influx.InfluxTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import com.td.sample.data.SimulationDataService;
import com.td.util.TimeUtil;

import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

@Service
public class InfluxDataService {
	private final static Logger logger = LoggerFactory.getLogger(InfluxDataService.class);
	@Autowired(required = false)
	private InfluxTemplate influxTemplate;

	private String dbName = "yd_data";

	@PostConstruct
	public void init() {
		SimulationDataService.dbEventBus.register(this);
	}

	@Subscribe
	public void revData(JSONObject data) {
		try {
			BatchPoints batchPoints = BatchPoints.database(dbName).retentionPolicy("autogen").build();
			Builder b = Point.measurement("data").time(TimeUtil.softParseDate(data.getString("time"), new Date()).getTime(), TimeUnit.MILLISECONDS)
				.tag("dev_type", data.getString("dev_type")).tag("psn", data.getString("psn"));
			for (String key : data.keySet()) {
				if (StringUtils.equals(key, "psn"))
					continue;
				if (StringUtils.equals(key, "time"))
					continue;
				if (StringUtils.equals(key, "dev_type"))
					continue;
				b.addField(key, data.getDouble(key));
			}
			Point point = b.build();
			batchPoints.point(point);
			influxTemplate.getInfluxDB().write(batchPoints);
			logger.debug("data:{}", data);
		} catch (Exception e) {
			logger.error("influx错误", data);
		}
	}
	
	
	

}
