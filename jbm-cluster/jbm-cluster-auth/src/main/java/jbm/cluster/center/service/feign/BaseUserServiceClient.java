package jbm.cluster.center.service.feign;

import com.jbm.cluster.api.constants.BaseConstants;
import com.jbm.cluster.api.service.IBaseUserServiceClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

/**
 * @author: wesley.zhang
 * @date: 2018/10/24 16:49
 * @description:
 */
@Component
@FeignClient(value = BaseConstants.BASE_SERVER)
public interface BaseUserServiceClient extends IBaseUserServiceClient {


}
