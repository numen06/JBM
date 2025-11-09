package com.jbm.cluster.logs.form;

import com.jbm.cluster.logs.entity.BusinessLog;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 业务日志查询表单
 * 
 * @author wesley
 */
@Data
@ApiModel(value = "业务日志查询表单")
public class BusinessLogForm {
    
    @ApiModelProperty(value = "业务日志查询实体")
    private BusinessLog businessLog;
    
    @ApiModelProperty(value = "分页对象")
    private PageForm pageForm;
    
    @ApiModelProperty(value = "开始时间")
    private Date beginTime;
    
    @ApiModelProperty(value = "结束时间")
    private Date endTime;
    
    @ApiModelProperty(value = "业务日志ID")
    private String logId;
    
    @ApiModelProperty(value = "业务模块")
    private String module;
    
    @ApiModelProperty(value = "用户ID")
    private String userId;
    
    @ApiModelProperty(value = "日志级别")
    private String level;
    
    @ApiModelProperty(value = "追踪ID")
    private String traceId;
    
    @ApiModelProperty(value = "关键词搜索")
    private String keyword;
}

