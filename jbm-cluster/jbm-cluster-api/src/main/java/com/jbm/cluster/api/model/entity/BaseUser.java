package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.annotation.TableAlias;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * 系统用户-基础信息
 *
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableAlias("user")
@TableName("base_user")
@ApiModel("系统用户")
public class BaseUser extends MasterDataEntity {
    private static final long serialVersionUID = -735161640894047414L;
    /**
     * 系统用户ID
     */
    @Id
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("用户ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    /**
     * 登陆名
     */
    @ApiModelProperty("登录名")
    private String userName;

    /**
     * 用户类型:super-超级管理员 normal-普通管理员
     */
    @ApiModelProperty("用户类型:super-超级管理员 normal-普通管理员")
    private String userType;

    /**
     * 组织架构企业ID
     */
    @ApiModelProperty("公司ID")
    private Long companyId;

    @ApiModelProperty("部门ID")
    private Long departmentId;

    /**
     * 昵称
     */
    @ApiModelProperty("昵称")
    private String nickName;

    /**
     * 登陆名
     */
    @ApiModelProperty("真实姓名")
    private String realName;

    /**
     * 头像
     */
    @ApiModelProperty("头像")
    private String avatar;

    /**
     * 邮箱
     */
    @ApiModelProperty("邮箱")
    private String email;

    /**
     * 手机号
     */
    @ApiModelProperty("手机号")
    private String mobile;

    /**
     * 描述
     */
    @ApiModelProperty("描述")
    private String userDesc;

    /**
     * 密码
     */
    @JsonIgnore
    @TableField(exist = false)
    @ApiModelProperty("密码")
    private String password;

    /**
     * 状态:0-禁用 1-正常 2-锁定
     */
    @ApiModelProperty("状态:0-禁用 1-正常 2-锁定")
    private Integer status;
}
