package com.td.sample.pr;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

@Component
public class UdpToString implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		String temp = StringUtils.trimToNull(StringUtils.substringBetween(exchange.getIn().getBody().toString(), ",", ")"));
		PrUdpForm form = JSON.parseObject(temp, PrUdpForm.class);
		System.out.println(JSON.toJSONString(form));
	}

}
