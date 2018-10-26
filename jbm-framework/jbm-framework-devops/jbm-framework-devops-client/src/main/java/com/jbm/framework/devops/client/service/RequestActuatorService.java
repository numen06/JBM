/**
 * 
 */
package com.jbm.framework.devops.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jbm.devops.bean.ReleaseOption;
import com.jbm.framework.devops.client.bean.ConfigOption;

import jbm.framework.boot.autoconfigure.rest.RealRestTemplate;
import jbm.framework.boot.autoconfigure.rest.RestTemplateFactory;

/**
 * @author Leonid
 *
 */
@Service
public class RequestActuatorService {

	private static final Logger logger = LoggerFactory.getLogger(RequestActuatorService.class);

	private ConfigOption config = ConfigOption.getInstance();
	
	private RealRestTemplate realRestTemplate = RestTemplateFactory.getInstance().createRealRestTemplate();

	/**
	 * 
	 */
	public RequestActuatorService() {
		// TODO Auto-generated constructor stub
	}

	public void publishSource(ReleaseOption option) {
//		HttpResponse httpResponse = HttpRequest.post(config.getUrl()).form("ope", ope).form("json", json).send();
		String fileResponse =  realRestTemplate.postJsonForObject(config.getUrl(), option, String.class);
		logger.info("return {}", fileResponse);
	}

}
