package com.jbm.framework.masterdata.usage.entity;


import io.swagger.annotations.ApiModelProperty;

/***
 * 多租户平台基础对象
 */
public class MultiPlatformTreeEntity extends MasterDataTreeEntity {

    @ApiModelProperty("应用ID")
    private Long applicationId;
}
