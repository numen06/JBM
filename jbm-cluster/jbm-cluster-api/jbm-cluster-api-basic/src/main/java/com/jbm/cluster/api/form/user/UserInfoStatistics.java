package com.jbm.cluster.api.form.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UserInfoStatistics {

    @ApiModelProperty("在线用户数量")
    private Long onlineUser;
    @ApiModelProperty("用户总量")
    private Long usersTotal;

}
