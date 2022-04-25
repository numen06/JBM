package com.jbm.framework.eventbus.example.event;

import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@RemoteApplicationEventScan(basePackageClasses = TestRemoteEvent.class)
public class BusConfiguration {
}
