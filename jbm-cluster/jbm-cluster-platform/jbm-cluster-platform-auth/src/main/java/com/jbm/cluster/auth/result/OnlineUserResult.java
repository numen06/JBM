package com.jbm.cluster.auth.result;

import com.jbm.cluster.api.model.auth.SysUserOnline;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 在线用户列表响应。
 *
 * <p>使用明确的字段白名单，避免将缓存对象中的认证凭据暴露给客户端。</p>
 */
@Data
@ApiModel("在线用户列表响应")
public class OnlineUserResult implements Serializable {

    @ApiModelProperty(value = "部门名称")
    private String deptName;

    @ApiModelProperty(value = "用户名称")
    private String userName;

    @ApiModelProperty(value = "登录IP地址")
    private String ipaddr;

    @ApiModelProperty(value = "登录地址")
    private String loginLocation;

    @ApiModelProperty(value = "浏览器类型")
    private String browser;

    @ApiModelProperty(value = "操作系统")
    private String os;

    @ApiModelProperty(value = "登录时间")
    private Date loginTime;

    @ApiModelProperty(value = "过期时间")
    private Date expiredTime;

    @ApiModelProperty(value = "临时有效期")
    private Date activityTime;

    public static OnlineUserResult from(SysUserOnline source) {
        OnlineUserResult result = new OnlineUserResult();
        result.setDeptName(source.getDeptName());
        result.setUserName(source.getUserName());
        result.setIpaddr(source.getIpaddr());
        result.setLoginLocation(source.getLoginLocation());
        result.setBrowser(source.getBrowser());
        result.setOs(source.getOs());
        result.setLoginTime(source.getLoginTime());
        result.setExpiredTime(source.getExpiredTime());
        result.setActivityTime(source.getActivityTime());
        return result;
    }
}
