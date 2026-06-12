package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseUser;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-02-29 20:07
 **/
@ApiModel("用户提交表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseUserForm extends BaseUser {
    @ApiModelProperty("权限ID数组")
    private String[] roleIds;
    @ApiModelProperty("旧密码")
    private String originPassword;
    @ApiModelProperty("新密码")
    private String currentPassword;
    @ApiModelProperty("确认密码")
    private String confirmPassword;
    @ApiModelProperty("时间范围")
    private Date[] dateRange;
    @ApiModelProperty("开始时间")
    private Date beginTime;
    @ApiModelProperty("结束时间")
    private Date endTime;
    @ApiModelProperty("部门ID列表（查询用）")
    private List<Long> departmentIds;
    @ApiModelProperty("是否精确匹配部门ID")
    private Boolean exactDepartment;
}
