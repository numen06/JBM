package jbm.cluster.center.service.feign;

import com.jbm.cluster.api.constants.BaseConstants;
import com.jbm.cluster.api.service.IBaseAppServiceClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

/**
 * @author: liuyadu
 * @date: 2018/10/24 16:49
 * @description:
 */
@Component
@FeignClient(value = BaseConstants.BASE_SERVER)
public interface BaseAppServiceClient extends IBaseAppServiceClient {


}
