package com.jbm.cluster.api.entitys.job.rule;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.*;

/**
 * @author scolin
 * @description 业务节点
 * @date 2025/11/18 18:01
 */
@Data
@ApiModel("业务节点")
//@Table
//@Entity
public class BusinessNode {
    @Id
    @ApiModelProperty("业务节点id")
    private String id;
    @ApiModelProperty("节点类型")
    private String nodeType;
    @ApiModelProperty("请求地址")
    private String url;
    @ApiModelProperty("请求方式")
    private String method;
}
