package com.jbm.cluster.push.form;

import com.jbm.cluster.api.entitys.message.PushMessage;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@ApiModel("站内信表单")
public class PushMessageForm {

    private PushMessage pushMessage;
    private PageForm pageForm;
}
