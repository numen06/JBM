package com.jbm.cluster.logs.entity;

import com.jbm.cluster.api.model.GatewayLogInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 16:52
 **/
@Data
@Document("gateway_logs")
public class GatewayLogs extends GatewayLogInfo {


}
