package com.jbm.framework.opcua.attribute;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class OpcPointsRead {

    public Map<String, OpcPoint> readPoints(String fileName) {
        Map<String, OpcPoint> points = Maps.newConcurrentMap();
        try {
            CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
            csvReadConfig.setContainsHeader(true);
            ClassPathResource resource = new ClassPathResource(fileName);
            log.info("开始读取文件:{}", resource.getAbsolutePath());
            CsvData csvData = CsvUtil.getReader(csvReadConfig).read(resource.getReader(Charsets.UTF_8));
            csvData.forEach(new Consumer<CsvRow>() {
                @Override
                public void accept(CsvRow csvRow) {
                    Integer namespace = 3;
                    if (csvData.getHeader().contains("namespace")) {
                        String temp = csvRow.getByName("namespace");
                        if (StrUtil.isNotBlank(temp)) {
                            namespace = Integer.valueOf(csvRow.getByName("namespace"));
                        }
                    }
                    String tagName = csvRow.getByName("name");
                    String dataType = csvRow.getByName("type");
                    List<String> nameArr = StrUtil.split(tagName, ".");
                    //默认读取最后一个字段
                    String alias = CollUtil.getLast(nameArr);
                    //如果存在别名则读取
                    if (csvData.getHeader().contains("alias")) {
                        String temp = csvRow.getByName("alias");
                        if (StrUtil.isNotBlank(temp)) {
                            alias = temp;
                        }
                    }
                    tagName = StrUtil.concat(true, "\"", StrUtil.join("\".\"", nameArr), "\"");
                    OpcPoint attributeInfo = new OpcPoint(namespace, tagName, dataType);
                    attributeInfo.setAlias(alias);
                    points.put(alias, attributeInfo);
                }
            });
        } catch (Exception e) {
            log.error("加载点位错误", e);
        }
        log.info("成功读取点位[{}]个:{}", points.size(), JSON.toJSONString(points));
        return points;
    }
}
