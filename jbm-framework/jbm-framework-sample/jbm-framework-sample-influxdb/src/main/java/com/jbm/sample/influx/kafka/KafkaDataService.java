package com.jbm.sample.influx.kafka;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.influx.InfluxTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.util.TimeUtil;

//@Service
public class KafkaDataService {
	private final static Logger logger = LoggerFactory.getLogger(KafkaDataService.class);

	// private final KafkaTemplate kafkaTemplate;
	@Autowired
	private InfluxTemplate influxTemplate;

	private Properties pipe;

	private String dbName = "db1";

	@PostConstruct
	public void init() throws IOException {
		pipe = PropertiesLoaderUtils.loadProperties(new ClassPathResource("pipe.properties"));
	}

	@KafkaListener(topics = "SMART1")
	public void processMessage(String content) {
		// JSONObject obj = JSON.parseObject(content);
		BatchPoints batchPoints = BatchPoints.database(dbName).retentionPolicy("autogen").build();
		List<JSONObject> array = JSON.parseArray(content, JSONObject.class);

		for (JSONObject obj : array) {
			JSONObject data = obj.getObject("data", JSONObject.class);
			// if (!new Integer(6).equals(obj.getInteger("devType"))) {
			// continue;
			// }
			// Double v = data.getDouble("8254");
			// if (v == null)
			// continue;
			Point point = null;
			String dev_type = obj.getString("devType");
			if (dev_type == null)
				continue;
			Builder b = Point.measurement("data").time(TimeUtil.softParseDate(obj.getString("time"), new Date()).getTime(), TimeUnit.MILLISECONDS).tag("dev_type", dev_type)
				.tag("psn", obj.getString("pSn"));
			int index = 0;
			for (String key : data.keySet()) {
				String code = pipe.getProperty(dev_type + "-" + key);
				if (code == null)
					continue;
				Double value = data.getDouble(key);
				if (value == null)
					continue;
				b.addField(code, value);
				index++;
			}
			if (index > 0) {
				point = b.build();
				// logger.info(obj);
				// influxTemplate.getInfluxDB().write(dbName, "default",
				// InfluxDB.ConsistencyLevel.ONE,"");
				batchPoints.point(point);
			}
		}
		int size = batchPoints.getPoints().size();
		if (size <= 0)
			return;
		influxTemplate.getInfluxDB().write(batchPoints);
		logger.info("write data size : {}", size);
		// final QueryResult queryResult =
		// influxTemplate.getInfluxDB().query(new Query("select * from data
		// LIMIT 1", dbName));
		// logger.info(queryResult);
	}
	// logger.info(content);

}
