package org.springframework.data.influx;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.date.ZoneUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

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

    /**
     * 批量解析
     *
     * @param series
     * @return
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
//                relVal = Date.from(DateUtil.parseUTC(StrUtil.toString(val)).toLocalDateTime().atZone(ZonedDateTime.now().getZone()).toInstant());
                relVal = Date.from(DateUtil.parseUTC(StrUtil.toString(val)).toInstant().atOffset(ZonedDateTime.now().getOffset()).toInstant());
//                    relVal = fromInfluxDBTime(val.toString());
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
