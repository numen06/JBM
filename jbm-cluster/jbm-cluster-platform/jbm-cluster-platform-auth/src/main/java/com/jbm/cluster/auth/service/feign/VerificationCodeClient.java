package com.jbm.cluster.auth.service.feign;

import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author fyk
 */
@FeignClient(name = VerificationCodeClient.SERVICE_ID)
public interface VerificationCodeClient {
    /**
     * eureka service name
     */
    String SERVICE_ID = "common-service";
    /**
     * common api prefix
     */
    String API_PATH = "/api/v1";

    @RequestMapping(value = "/common/vcc/token", method = RequestMethod.GET)
    ResultBody<String> getToken(@RequestParam("size") Integer size,
                                @RequestParam("expire") Long expire,
                                @RequestParam("type") String type,
                                @RequestParam("subject") String subject,
                                @RequestParam("sendSms") Boolean sendSms);

    @RequestMapping(value = "/common/vcc/validate", method = RequestMethod.GET)
    ResultBody<Boolean> validate(@RequestParam("token") String token, @RequestParam("code") String code, @RequestParam("subject") String subject);

}
