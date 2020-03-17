package com.jbm.framework.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-16 01:57
 **/
@Data
@ApiModel("传递Ids")
public class IdsForm {

    @ApiModelProperty("ID数组")
    private Long[] ids;
}
