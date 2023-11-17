package org.springframework.data.influx;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 根据数据解析
 *
 * @author wesley.zhang
 */
@Slf4j
public class InfluxDataDeserializer {

    @SuppressWarnings("unused")
    private Class<?> clazz = Map.class;

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
                log.error("查询influxdb发生错误:{}", result.getError());
                return list;
            }
            if (CollUtil.isEmpty(result.getSeries())) {
                return list;
            }
            for (Series series : result.getSeries()) {
                serializeSeries(list, series);
            }
        }
        return list;
    }

    public <T> List<T> deserializerObject(QueryResult queryResult) {
        List<Map<String, Object>> list = this.deserializer(queryResult);
        List<T> result = list.stream().map(new Function<Map<String, Object>, T>() {
            @Override
            public T apply(Map<String, Object> stringObjectMap) {
                if (clazz.equals(Map.class)) {
                    return (T) stringObjectMap;
                }
                JSONObject jsonObject = new JSONObject(stringObjectMap);
                return (T) jsonObject.toJavaObject(clazz);
            }
        }).collect(Collectors.toList());
        return result;
    }


    /**
     * 批量解析
     *
     * @param series
     * @return
     * @throws ParseException
     */
    public List<Map<String, Object>> serializeSeries(List<Map<String, Object>> list, Series series) {
        List<String> columns = series.getColumns();
        Map<String, String> tags = series.getTags();
        List<List<Object>> values = series.getValues();
        for (List<Object> row : values) {
            list.add(serializRow(tags, columns, row));
        }
        return list;
    }

    public Map<String, Object> serializRow(Map<String, String> tags, List<String> columns, List<Object> row) {
        Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
        if (MapUtil.isNotEmpty(tags)) {
            resultMap.putAll(tags);
        }
        Object relVal = null;
        for (int j = 0; j < row.size(); j++) {
            String col = columns.get(j);
            Object val = row.get(j);
            if (col.equals("time")) {
                relVal = InfluxDateUtil.formUtcToDate(StrUtil.toString(val));
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

//    public static void main(String[] args) {
////        String str = "2021-12-30T06:17:49.011Z";
//        String str = "2022-03-04T08:00:25Z";
//        System.out.println(DateUtil.parseUTC(str));
//        System.out.println(LocalDateTimeUtil.of(new Date(1646351125007l)).atZone(ZonedDateTime.now().getZone()));
//        System.out.println(ZonedDateTime.now().getZone());
//        System.out.println(DateUtil.parseUTC(str).getZoneId());
//        System.out.println(DateUtil.parseUTC(str).toLocalDateTime().atZone(ZonedDateTime.now().getZone()));
//        System.out.println(DateUtil.parseUTC(str).toInstant().atOffset(ZonedDateTime.now().getOffset()));
//    }


}
