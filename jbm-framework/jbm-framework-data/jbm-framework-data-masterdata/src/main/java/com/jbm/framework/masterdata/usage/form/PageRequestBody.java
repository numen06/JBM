package com.jbm.framework.masterdata.usage.form;

import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.usage.form.BaseRequsetBody;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-19 21:31
 **/
@Data
@ApiModel(value = "分页请求实体")
public class PageRequestBody extends BaseRequsetBody {


    /**
     * 分页封装类
     */
    @ApiModelProperty(value = "分页参数")
    private PageParams pageParams;
    /**
     * 分页封装类
     */
    @ApiModelProperty(value = "分页封装实体")
    private PageForm pageForm;

    public PageRequestBody() {
        super();
    }

    public PageRequestBody(Map map) {
        super(map);
    }

    public PageRequestBody(PageParams pageParams) {
        this.pageParams = pageParams;
    }

    public static PageRequestBody from(Map map) {
        return new PageRequestBody(map);
    }

    public static PageRequestBody from(PageParams pageParams) {
        return new PageRequestBody(pageParams);
    }

    public PageParams getPageParams() {
        if (this.containsClass(PageForm.class)) {
            this.pageParams = PageParams.from(this.getPageForm());
        } else {
            this.pageParams = PageParams.from(this);
        }
        return this.pageParams;
    }

    public PageForm getPageForm() {
        if (this.pageForm == null) {
            this.pageForm = this.tryGet(PageForm.class);
        }
        return this.pageForm;
    }

}
