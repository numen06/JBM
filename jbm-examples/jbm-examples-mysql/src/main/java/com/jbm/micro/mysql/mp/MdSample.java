package com.jbm.micro.mysql.mp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 主表示例：表结构由 Liquibase 管理，业务读写仅通过 MyBatis-Plus（不再使用 JPA）。
 */
@Data
@TableName("md_sample")
public class MdSample implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("form_json")
    private String formJson;
}
