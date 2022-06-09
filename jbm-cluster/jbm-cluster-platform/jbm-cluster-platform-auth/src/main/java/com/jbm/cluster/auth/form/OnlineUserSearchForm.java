package com.jbm.cluster.auth.form;

import com.jbm.framework.usage.form.PageSearchForm;
import com.jbm.framework.usage.paging.PageForm;
import lombok.Data;

/**
 * @Created wesley.zhang
 * @Date 2022/6/8 10:16
 * @Description TODO
 */
@Data
public class OnlineUserSearchForm extends PageSearchForm {

    private String ipaddr;
    private String userName;
}
