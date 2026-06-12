package com.jbm.cluster.api.constants;

/**
 * 当前登录人组织用户查询范围
 */
public enum OrgUserScope {
    /**
     * 同公司（顶层组织）
     */
    COMPANY,
    /**
     * 同部门（精确匹配）
     */
    DEPARTMENT,
    /**
     * 同部门及所有子部门
     */
    DEPARTMENT_TREE
}
