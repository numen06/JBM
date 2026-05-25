package com.jbm.cluster.common.mysql.service;

import lombok.Data;

/**
 * 在线用户列表查询条件（与 auth 模块 OnlineUserSearchForm 对齐）。
 */
@Data
public class OnlineUserFilter {

    private String ipaddr;
    private String userName;
    private Long appId;
    private Long companyId;
}
