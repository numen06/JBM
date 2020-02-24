package com.jbm.framework.usage.form;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.StringUtils;
import com.jbm.util.json.JSONBean;

import java.util.List;
import java.util.Map;

/**
 * 前端请求封装类
 *
 * @author wesley.zhang
 */
public class JsonRequestBody extends BaseRequsetBody {


    /**
     * 分页封装类
     */
    private PageForm pageForm;

    public PageForm getPageForm() {
        if (this.pageForm == null) {
            this.pageForm = this.tryGet(PageForm.class);
        }
        return this.pageForm;
    }

}
