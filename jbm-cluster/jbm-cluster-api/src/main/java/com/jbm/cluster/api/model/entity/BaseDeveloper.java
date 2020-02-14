package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.persistence.Entity;

/**
 * 系统用户-管理员信息
 *
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_developer")
public class BaseDeveloper extends MasterDataEntity {

    /**
     * 登陆名
     */
    private String userName;

    /**
     * 开发者类型:isp-服务提供商 dev-自研开发者
     */
    private String userType;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 描述
     */
    private String userDesc;

    /**
     * 密码
     */
    @JsonIgnore
    @TableField(exist = false)
    private String password;

    /**
     * 状态:0-禁用 1-正常 2-锁定
     */
    private Integer status;
}
