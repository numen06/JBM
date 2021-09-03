package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

/**
 * 组织结构
 *
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-24 03:08
 **/
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_org")
@ApiModel(value = "组织结构")
public class BaseOrg extends MasterDataTreeEntity {

    @ApiModelProperty(value = "组织名称")
    private String orgName;

    /**
     * 组织类别 by wesley.zhang on 2019/03/22 15:15
     * 组织类别
     * 组织类别:关联字典org_type
     */
    @ApiModelProperty(value = "组织类型:company,department")
    private String orgType;


    /**
     * 部门主管 by wesley.zhang on 2019/04/02 10:09
     * 部门主管
     * 部门经理，部门领导
     */
    @ApiModelProperty(value = "组织负责人")
    private Long managerId;


    /**
     * 源ID by wesley.zhang on 2019/03/22 15:18
     * 源ID（深圳）
     * 深圳项目使用，主要做数据同步用
     */
    @ApiModelProperty(value = "组织名称")
    private String sourceId;


    /**
     * 组ID by wesley.zhang on 2019/04/02 10:03
     * 组ID
     * 深圳项目使用
     */
    @ApiModelProperty(value = "分组ID")
    private String groupId;


    /**
     * 分管领导 by wesley.zhang on 2019/04/02 10:00
     * 分管领导
     * 部门分管上级领导
     */
    @ApiModelProperty(value = "组长")
    private Long leaderId;


    /**
     * 单位代码 by wesley.zhang on 2019/04/03 13:57
     * 单位代码
     * 单位代码
     */
    @ApiModelProperty(value = "组织代码")
    private String orgCode;
}
