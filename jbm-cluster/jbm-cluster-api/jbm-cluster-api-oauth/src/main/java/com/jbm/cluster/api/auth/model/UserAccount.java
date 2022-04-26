package com.jbm.cluster.api.auth.model;

import com.google.common.collect.Lists;
import com.jbm.cluster.api.auth.OpenAuthority;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import lombok.Data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * @author: wesley.zhang
 * @date: 2018/11/12 11:35
 * @description:
 */
@Data
public class UserAccount extends BaseAccount implements Serializable {
    private static final long serialVersionUID = 6717800085953996702L;

    private Collection<Map> roles = Lists.newArrayList();
    /**
     * 用户权限
     */
    private Collection<OpenAuthority> authorities = Lists.newArrayList();
    /**
     * 第三方账号
     */
    private String thirdParty;
    /**
     * 昵称
     */
    private String nickName;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 真实名字
     */
    private String realName;
    /**
     * 手机号
     */
    private String mobile;

    /**
     * 公司
     */
    private Long companyId;

    /**
     * 部门
     */
    private Long departmentId;


}
