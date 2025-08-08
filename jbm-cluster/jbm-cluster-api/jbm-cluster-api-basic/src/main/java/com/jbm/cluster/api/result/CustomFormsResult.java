package com.jbm.cluster.api.result;

import com.jbm.cluster.api.form.center.CustomFormsForm;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author scolin
 * @description
 * @date 2025/7/23 16:37
 */
@Data
@ApiModel("自定义表单响应体")
public class CustomFormsResult extends CustomFormsForm {
}
