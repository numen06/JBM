package com.jbm.cluster.api.entitys.center;


import com.jbm.cluster.api.constants.center.DataSourceType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author scolin
 * @description
 * @date 2025/7/23 17:50
 */
@Data
@ApiModel("数据源管理")
@Entity
@Table
public class DataSourceManagement extends MasterDataEntity {

    @ApiModelProperty("数据源编码")
    @NotBlank(message = "数据源编码不能为空")
    private String dataSourceCode;
    @ApiModelProperty("数据源名称")
    @NotBlank(message = "数据源名称不能为空")
    private String dataSourceName;
    @ApiModelProperty("数据源类型 字典 data_source_type")
    @Enumerated(EnumType.STRING)
    @NotNull(message = "数据源类型不能为空")
    private DataSourceType dataSourceType;
    @ApiModelProperty("自定义选项内容")
    @Lob
    @Column(columnDefinition = "TEXT")
    private String customizeContent;
    @ApiModelProperty("URL地址")
    private String url;
    @ApiModelProperty("请求方式 字典 request_method")
    @Enumerated(EnumType.STRING)
    private RequestMethod requestMethod;
    @ApiModelProperty("请求头")
    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestHeader;
    @ApiModelProperty("请求体")
    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestBody;
}
