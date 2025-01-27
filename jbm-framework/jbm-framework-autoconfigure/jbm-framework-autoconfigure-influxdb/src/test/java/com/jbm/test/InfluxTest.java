package com.jbm.test;

import jbm.framework.boot.autoconfigure.influx.InfluxProperties;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.influx.InfluxTemplate;
import org.springframework.data.influx.SimpleInfluxTemplate;

public class InfluxTest {

    private SimpleInfluxTemplate influxTemplate;


    @BeforeEach
    public void init() {
        InfluxProperties influxProperties = new InfluxProperties();
        influxProperties.setUrl("http://10.100.10.62:8086");
        influxProperties.setUsername("root");
        influxProperties.setPassword("root");
        influxProperties.setDatabase("test1");
        this.influxTemplate = new SimpleInfluxTemplate(influxProperties);
    }

    @Test
    public void testInit() {
    }


}
