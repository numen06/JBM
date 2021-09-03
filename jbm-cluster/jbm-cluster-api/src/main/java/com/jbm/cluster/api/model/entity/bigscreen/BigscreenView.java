package com.jbm.cluster.api.model.entity.bigscreen;


import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("大屏管理")
@TableName("bigscreen_view")
public class BigscreenView extends MasterDataIdEntity {

    @ApiModelProperty(value = "大屏名称")
    private String viewName;
    @ApiModelProperty(value = "访问地址")
    private String viewUrl;
    @ApiModelProperty(value = "访问地址")
    private String staticParams;
    @ApiModelProperty(value = "资源包地址")
    private String resourcePath;
    @ApiModelProperty(value = "预览图")
    private String previewPicture;
    @ApiModelProperty(value = "版本号")
    private String version;
    @Column(columnDefinition = "TEXT")
    @ApiModelProperty(value = "配置文件")
    private String configData;


}
