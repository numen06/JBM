package com.jbm.cluster.api.model.entity;

import com.jbm.cluster.api.constants.AreaType;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * 行政区域管理
 *
 * @author wesley.zhang
 */
@Data
@Entity
@NoArgsConstructor
@ApiModel("行政区域")
@EqualsAndHashCode(callSuper = true)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"areaCode"}))
public class BaseArea extends MasterDataIdEntity {

    @ApiModelProperty(value = "区域编码")
    private String areaCode;
    @ApiModelProperty(value = "区域名称")
    private String areaName;
    @ApiModelProperty(value = "上级节点")
    private String parentCode;
    @ApiModelProperty(value = "区域类型")
    private AreaType areaType;
    @ApiModelProperty(value = "全拼")
    private String fullPinYin;
    @ApiModelProperty(value = "简拼")
    private String simplePinYin;
    //    @ApiModelProperty("国家")
//    private String country;
//    @ApiModelProperty("板块")
//    private String plate;
//    @ApiModelProperty("省")
//    private String province;
//    @ApiModelProperty("市")
//    private String city;
//    @ApiModelProperty("区")
//    private String district;
    @ApiModelProperty("中心经纬度")
    private String centerLocation;
}
