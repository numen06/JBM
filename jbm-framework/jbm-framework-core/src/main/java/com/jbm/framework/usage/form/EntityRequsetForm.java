package com.jbm.framework.usage.form;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-02-19 21:28
 **/
@Data
@NoArgsConstructor
@ApiModel(value = "实体请求表单")
public class EntityRequsetForm<Entity> extends BaseRequestForm {

    private static final long serialVersionUID = 1L;
    private Entity entity;

    public Entity tryGet(Class<Entity> clazz) {
        if (ObjectUtil.isNotEmpty(entity)) {
            return entity;
        }
        return BeanUtil.getProperty(this, clazz.getSimpleName());
    }

    public List<Entity> tryGetList(Class<Entity> clazz) {
        return BeanUtil.getProperty(this, clazz.getSimpleName() + "s");
    }

}
