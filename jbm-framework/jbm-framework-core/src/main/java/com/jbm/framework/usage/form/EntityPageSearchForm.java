package com.jbm.framework.usage.form;

import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <pre>
 * 对数据进行封装的基础类
 * 特别注意PageForm开始页是1
 * </pre>
 *
 * @author wesley
 */
@Data
@ApiModel(value = "分页查询")
public class EntityPageSearchForm<Entity> extends EntityRequsetForm<Entity> {

    @ApiModelProperty(value = "分页对象")
    private PageForm pageForm;

}
