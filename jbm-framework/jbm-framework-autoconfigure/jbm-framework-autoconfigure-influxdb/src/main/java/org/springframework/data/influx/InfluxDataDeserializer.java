package org.springframework.data.influx;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jbm.framework.exceptions.ServiceException;

/**
 * 根据数据解析
 * 
 * @author wesley.zhang
 *
 */
public class InfluxDataDeserializer {

	private static Logger logger = LoggerFactory.getLogger(InfluxDataDeserializer.class);

	@SuppressWarnings("unused")
	private Class<?> clazz = Map.class;

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	private Map<String, Object> supplementColumns;

	public InfluxDataDeserializer(Class<?> clazz) {
		super();
		this.clazz = clazz;
	}

	public InfluxDataDeserializer(Class<?> clazz, Map<String, Object> supplementColumns) {
		super();
		this.clazz = clazz;
		this.supplementColumns = supplementColumns;
	}

	public List<Map<String, Object>> deserializer(QueryResult queryResult) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (Result result : queryResult.getResults()) {
			if (result.getError() != null) {
				logger.error(result.getError());
				return list;
			}
			for (Series series : result.getSeries()) {
				serializeSeries(list, series);
			}
		}
		return list;
	}

	/**
	 * 批量解析
	 * 
	 * @param series
	 * @return
	 * @throws ServiceException
	 * @throws ParseException
	 */
	public List<Map<String, Object>> serializeSeries(List<Map<String, Object>> list, Series series) {
		List<String> columns = series.getColumns();
		List<List<Object>> values = series.getValues();
		for (List<Object> row : values) {
			list.add(serializRow(columns, row));
		}
		return list;
	}

	public Map<String, Object> serializRow(List<String> columns, List<Object> row) {
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
		Object relVal = null;
		for (int j = 0; j < row.size(); j++) {
			String col = columns.get(j);
			Object val = row.get(j);
			if (col.equals("time")) {
				try {
					relVal = fromInfluxDBTime(val.toString());
				} catch (ParseException e) {
					continue;
				}
			} else {
				relVal = val;
			}
			resultMap.put(col, relVal);
			if (supplementColumns != null) {
				for (String column : supplementColumns.keySet()) {
					resultMap.put(column, supplementColumns.get(column));
				}
			}
		}
		return resultMap;
	}

	public static Date fromInfluxDBTime(String time) throws ParseException {
		// 格式化时间
		Date date = sdf.parse(time);
		date = changeTimeZone(date, TimeZone.getTimeZone("GMT"), TimeZone.getDefault());
		return date;
	}

	public static String toInfluxDBTime(Date time) throws ParseException {
		// 格式化时间
		time = changeTimeZone(time, TimeZone.getDefault(), TimeZone.getTimeZone("GMT"));
		return sdf.format(time);
	}

	/**
	 * 获取更改时区后的日期
	 * 
	 * @param date
	 *            日期
	 * @param oldZone
	 *            旧时区对象
	 * @param newZone
	 *            新时区对象
	 * @return 日期
	 */
	public static Date changeTimeZone(Date date, TimeZone oldZone, TimeZone newZone) {
		Date dateTmp = null;
		if (date != null) {
			int timeOffset = oldZone.getRawOffset() - newZone.getRawOffset();
			dateTmp = new Date(date.getTime() - timeOffset);
		}
		return dateTmp;
	}

}
