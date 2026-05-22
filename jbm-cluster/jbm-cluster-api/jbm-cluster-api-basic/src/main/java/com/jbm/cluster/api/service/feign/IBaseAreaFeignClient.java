package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.entitys.basic.BaseArea;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

public interface IBaseAreaFeignClient {
    @GetMapping("/getChinaAreaList")
    List<BaseArea> getChinaAreaList();
}