package com.jbm.framework.masterdata.usage.entity;

import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import lombok.Data;

import java.util.Date;

/**
 * 用户基本的数据
 */
@Data
public class UserMasterdata extends MasterDataEntity {

    /**
     * 密码
     */
    private String password;
    /**
     * 随机盐
     */
    private String salt;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 修改时间
     */
    private Date updateTime;
    /**
     * 手机号
     */
    private String mobilePhone;
    /**
     * 头像
     */
    private String userHead;
}
