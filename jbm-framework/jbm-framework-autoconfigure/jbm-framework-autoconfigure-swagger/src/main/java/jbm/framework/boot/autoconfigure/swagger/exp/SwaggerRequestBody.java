package jbm.framework.boot.autoconfigure.swagger.exp;

import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-21 15:51
 **/
@Data
@ApiModel("动态请求体")
public class SwaggerRequestBody {

    /**
     * 分页封装类
     */
    @ApiModelProperty(value = "分页封装实体")
    private PageForm pageForm;

}
