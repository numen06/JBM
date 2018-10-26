package com.jbm.sample.data;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.googlecode.aviator.AviatorEvaluator;
import com.jbm.util.TimeUtil;

import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

@Service
@EnableScheduling
public class SimulationDataService extends AbstractScheduledService {

	private final static Logger logger = LoggerFactory.getLogger(SimulationDataService.class);
	
	public static final EventBus eventBus = new AsyncEventBus(Executors.newFixedThreadPool(10));

	public static final EventBus dbEventBus = new AsyncEventBus(Executors.newFixedThreadPool(10));

	public static final EventBus dbTempEventBus = new AsyncEventBus(Executors.newFixedThreadPool(10));

	// public static final EventBus eventBus = new EventBus();

	private Properties deviceProperties;

	private Map<String, Properties> deviceTypeProperties = Maps.newConcurrentMap();

	static {
		AviatorEvaluator.addFunction(new DriftFunction());
		AviatorEvaluator.addFunction(new RandomFunction());
		AviatorEvaluator.addFunction(new AccumulateFunction());
	}

	// private Properties buildValue(JSONObject data, Properties pp) {
	// return buildValue(data, pp, data);
	// }

	private Properties buildValue(JSONObject data, Properties pp) {
		Properties ppp = new Properties();
		// synchronized (data) {
		for (Object filed : pp.keySet()) {
			try {
				String rule = pp.get(filed.toString()).toString();
				if (StringUtils.contains(rule, filed.toString()) && !data.containsKey(filed.toString())) {
					data.put(filed.toString(), 0);
				}
				data.put(filed.toString(), new BigDecimal(AviatorEvaluator.execute(rule, data, false).toString()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			} catch (Exception e) {
				// System.out.println(e);
				// old.put(filed.toString(), 0);
				ppp.put(filed, pp.get(filed.toString()));
			}
		}
		// }
		return ppp;
	}

	// private Map<Object, Object> buildValue(JSONObject data, Map<Object,
	// Object> pp) {
	// Map<Object, Object> errors = Maps.newConcurrentMap();
	// for (Object filed : pp.keySet()) {
	// try {
	// data.put(filed.toString(),
	// AviatorEvaluator.exec(pp.get(filed.toString()).toString()));
	// } catch (Exception e) {
	// errors.put(filed, pp.get(filed.toString()));
	// }
	// }
	// return errors;
	// }

	private LoadingCache<String, JSONObject> cahceBuilder = CacheBuilder.newBuilder().refreshAfterWrite(3, TimeUnit.SECONDS).build(new CacheLoader<String, JSONObject>() {
		@Override
		public JSONObject load(String psn) throws Exception {
			JSONObject data = new JSONObject();
			data.put("psn", psn);
			data.put("time", TimeUtil.format());
			data.put("dev_type", deviceProperties.get(psn));
			Properties deviceTypeP = deviceTypeProperties.get(data.getString("dev_type"));
			Properties temp = deviceTypeP;
			while (!temp.isEmpty()) {
				temp = buildValue(data, temp);
			}
			return data;
		}

		@Override
		public ListenableFuture<JSONObject> reload(String key, JSONObject oldValue) throws Exception {
			Properties deviceTypeP = deviceTypeProperties.get(oldValue.getString("dev_type"));
			// 设置数据时间
			oldValue.put("time", TimeUtil.format());
			// for (Object filed : deviceTypeP.keySet()) {
			// // Double old = oldValue.getDoubleValue(filed.toString());
			// oldValue.put(filed.toString(),
			// AviatorEvaluator.exec(deviceTypeP.getProperty(filed.toString()),
			// oldValue));
			// }
			Properties temp = deviceTypeP;
			while (!temp.isEmpty()) {
				temp = buildValue(oldValue, deviceTypeP);
			}
			return Futures.immediateCheckedFuture(oldValue);
		}

	});

	@PostConstruct
	public void init() throws IOException {
		loadPro();
		this.startAsync();
	}

	public void loadPro() throws IOException {
		System.out.println("load");
		deviceProperties = PropertiesLoaderUtils.loadProperties(new FileSystemResource("config/device.properties"));
		for (Object key : deviceProperties.keySet()) {
			String deviceType = deviceProperties.getProperty(key.toString());
			if (!deviceTypeProperties.containsKey(deviceType)) {
				Properties temp = PropertiesLoaderUtils.loadProperties(new FileSystemResource("config/device_type/" + deviceType + ".properties"));
				deviceTypeProperties.put(deviceType, temp);
			}
		}
	}

	/**
	 * 10分钟存一次数据
	 * 
	 * @throws ExecutionException
	 */
	@Scheduled(cron = "0 0/10 * * * ?")
	public void sendData() throws ExecutionException {
		String time = TimeUtil.format(new Date(), "yyyy-MM-dd HH:mm:00");
		for (Object key : deviceProperties.keySet()) {
			JSONObject json = cahceBuilder.get(key.toString());
			JSONObject json1 = new JSONObject();
			JSONObject json2 = new JSONObject();
			for (String value : json.keySet()) {
				json1.put(value, json.get(value));
				json2.put(value, json.get(value));
			}
			if (json != null) {
				json1.put("time", time);
				dbEventBus.post(json1);
				json2.put("time", TimeUtil.format(TimeUtil.getBeforeSec(TimeUtil.getDateFromStr(time), -1)));
				dbEventBus.post(json2);
			}
		}
	}

//	@Scheduled(cron = "0 0/3 * * * ?")
	public void sendData2() throws ExecutionException {
		for (Object key : deviceProperties.keySet()) {
			JSONObject json = cahceBuilder.get(key.toString());
			if (json != null) {
				dbTempEventBus.post(json);
			}
		}
	}

	/**
	 * 
	 * 三秒发一次MQTT
	 * 
	 * @throws Exception
	 */
	@Override
	protected void runOneIteration() throws Exception {
		for (Object key : deviceProperties.keySet()) {
			JSONObject json = cahceBuilder.get(key.toString());
			JSONObject json1 = new JSONObject();
			for (String value : json.keySet()) {
				json1.put(value, json.get(value));
			}
			if (json != null) {
				eventBus.post(json1);
			}
		}
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(0, 3, TimeUnit.SECONDS);
	}

}
