package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

/**
 * 系统资源-API接口
 *
 * @author: wesley.zhang
 * @description:
 */
@ApiModel(value = "全局字典")
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_dic")
public class BaseDic extends MasterDataTreeEntity {

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "编码")
    private String code;
    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "服务ID")
    private String serviceId;

    @ApiModelProperty(value = "应用ID")
    private Long appId;

}
