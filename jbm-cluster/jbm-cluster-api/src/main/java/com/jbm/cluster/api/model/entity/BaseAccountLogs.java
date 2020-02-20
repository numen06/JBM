package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.util.Date;

/**
 * 系统用户-登录日志
 * @author wesley.zhang
 */
@Data
@Entity
@NoArgsConstructor
@TableName("base_account_logs")
public class BaseAccountLogs extends MasterDataEntity {

    private Date loginTime;

    /**
     * 登录Ip
     */
    private String loginIp;

    /**
     * 登录设备
     */
    private String loginAgent;

    /**
     * 登录次数
     */
    private Integer loginNums;

    private Long userId;

    private String account;

    private String accountType;

    private String accountId;

    private String domain;
}
