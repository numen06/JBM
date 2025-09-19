package com.jbm.cluster.auth.form;

import com.jbm.framework.usage.form.PageSearchForm;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Created wesley.zhang
 * @Date 2022/6/8 10:16
 * @Description TODO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OnlineUserSearchForm extends PageSearchForm {

    private String ipaddr;
    private String userName;
}
