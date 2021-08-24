package com.jbm.cluster.push.fegin;

import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@FeignClient(value = "jbm-cluster-platform-logs",path = "/GatewayLogs")
public interface TestLogFegin {

     @PostMapping({"/findLogs"})
     ResultBody<DataPaging<GatewayLogsTest>> findLogs(@RequestBody(required = false) GatewayLogsTest gatewayLogsForm) ;
}
