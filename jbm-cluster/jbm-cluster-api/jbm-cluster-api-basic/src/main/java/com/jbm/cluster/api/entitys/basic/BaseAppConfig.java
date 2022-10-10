package com.jbm.cluster.api.entitys.basic;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * 系统应用-基础信息
 *
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("应用配置管理")
@Table(indexes = {@Index(name = "appKeyIndex", columnList = "appKey", unique = true)})
public class BaseAppConfig extends MasterDataEntity {
    @Id
    @TableId(type = IdType.INPUT)
    @ApiModelProperty(value = "应用ID")
    private Long appId;
    @ApiModelProperty(value = "应用KEY")
    private String appKey;
    /**
     * API访问key
     */
    @Column(columnDefinition = "TEXT")
    @ApiModelProperty(value = "配置内容")
    private String configContent;


}
