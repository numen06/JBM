package com.jbm.cluster.common.mysql.service;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户可见的在线会话范围。
 */
@Data
public class OnlineUserVisibleScope {

    private boolean admin;

    /** 仅可见本人会话 */
    private boolean selfOnly;

    private Long selfUserId;

    private Long companyId;

    private List<Long> companyIds;

    private List<Long> departmentIds;

    /** 搜索表单：按应用筛选 */
    private Long filterAppId;

    /** 搜索表单：超管按组织筛选 */
    private Long filterCompanyId;
}
