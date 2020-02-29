package com.jbm.cluster.api.form;

import com.jbm.cluster.api.model.entity.BaseUser;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-29 20:07
 **/
@ApiModel("用户提交表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseUserForm extends BaseUser {

    /**
     * 角色IDS
     */
    private String[] roleIds;
}
