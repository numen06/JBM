package com.jbm.framework.masterdata.usage.form;

import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.usage.form.BaseRequsetBody;
import com.jbm.framework.usage.paging.PageForm;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-19 21:31
 **/
@Data
public class PageRequestBody extends BaseRequsetBody {


    public static PageRequestBody from(Map map) {
        return new PageRequestBody(map);
    }

    public PageRequestBody() {
        super();
    }

    public PageRequestBody(Map map) {
        super(map);
    }


    /**
     * 分页封装类
     */
    public PageParams pageParams;


    /**
     * 分页封装类
     */
    public PageForm pageForm;

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
