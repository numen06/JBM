package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.util.Date;

/**
 * 系统用户-登录日志
 *
 * @author wesley.zhang
 */
@Data
@Entity
@NoArgsConstructor
@TableName("base_account_logs")
@ApiModel("登录日志")
@EqualsAndHashCode(callSuper = true)
public class BaseAccountLogs extends MasterDataIdEntity {
    @ApiModelProperty(value = "登录时间")
    private Date loginTime;
    @ApiModelProperty(value = "登录IP")
    private String loginIp;
    @ApiModelProperty(value = "登录地址")
    private String loginLocation;
    @ApiModelProperty(value = "浏览器")
    private String browser;
    @ApiModelProperty(value = "操作系统")
    private String os;
    @ApiModelProperty(value = "登录设备")
    private String loginAgent;
    @ApiModelProperty(value = "登录次数")
    private Integer loginNums;
    @ApiModelProperty(value = "登录用户")
    private Long userId;
    @ApiModelProperty(value = "登录帐户")
    private String account;
    @ApiModelProperty(value = "登录方式")
    private String accountType;
    @ApiModelProperty(value = "登录帐户ID")
    private String accountId;
    @ApiModelProperty(value = "登录域名")
    private String domain;
}
