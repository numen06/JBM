package com.jbm.sample.pr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

@Component
public class FileToUdp implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		// List<String> list =
		// JSON.parseArray(exchange.getContext().getTypeConverter().convertTo(String.class,
		// exchange.getIn().getBody()), String.class);
		String fileName = exchange.getIn().getBody(GenericFile.class).getFileName();
		List<PrFileForm> list = new ArrayList<PrFileForm>();
		PrFileForm form = new PrFileForm(fileName);
		list.add(form);
		Map<String, Object> info = new HashMap<>();
		info.put("info", list);
		String json = JSON.toJSONString(info); 
		System.out.println(json);
		exchange.getOut().setBody(json);
	}

}
