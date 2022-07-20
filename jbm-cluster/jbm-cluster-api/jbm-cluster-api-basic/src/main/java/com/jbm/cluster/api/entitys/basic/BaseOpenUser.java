package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 系统用户-基础信息
 *
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("开放用户信息")
@TableName
@Table(indexes = {@Index(name = "appIdUserIndex", columnList = "userId,appId", unique = true),
        @Index(name = "appKeyUserIndex", columnList = "userId,appKey", unique = true)})
public class BaseOpenUser extends MasterDataEntity {

    @Id
    @TableId(type = IdType.INPUT)
    @ApiModelProperty(value = "OPENID")
    private String openId;

    private Long appId;

    private String appKey;

    private Long userId;
}
