package com.jbm.framework.usage.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-19 21:28
 **/
@Data
@NoArgsConstructor
@ApiModel(value = "基础请求表单")
public class BaseRequestForm implements Serializable {

    private static final long serialVersionUID = 1L;


}
