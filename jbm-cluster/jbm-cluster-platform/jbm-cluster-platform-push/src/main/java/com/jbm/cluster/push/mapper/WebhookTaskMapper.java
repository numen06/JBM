package com.jbm.cluster.push.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.cluster.push.result.WebhookTaskReslut;
import com.jbm.framework.masterdata.annotation.MapperRepository;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
@MapperRepository
public interface WebhookTaskMapper extends SuperMapper<WebhookTask> {

    Page<WebhookTaskReslut> selectWebhookTasks(Page page, @Param("form") WebhookTaskForm webhookTaskForm);
}
