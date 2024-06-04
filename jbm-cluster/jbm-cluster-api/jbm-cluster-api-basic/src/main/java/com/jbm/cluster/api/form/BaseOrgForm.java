package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseOrg;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-02-29 20:07
 **/
@ApiModel("组织结构表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseOrgForm extends BaseOrg {

}
