package com.jbm.micro.mysql.mp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 多租户 + 字段演进示例表：{@code tenant_id} 由租户拦截器维护；{@code remark} 由 Liquibase V4 追加。
 */
@Data
@TableName("md_tenant_demo")
public class MdTenantDemo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long tenantId;

    @TableField("remark")
    private String remark;
}
