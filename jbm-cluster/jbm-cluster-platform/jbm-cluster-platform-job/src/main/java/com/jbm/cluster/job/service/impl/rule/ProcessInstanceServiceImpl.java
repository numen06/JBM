package com.jbm.cluster.job.service.impl.rule;

import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.job.service.rule.ProcessInstanceService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 15:56
 */
@Service
public class ProcessInstanceServiceImpl extends MasterDataServiceImpl<ProcessInstance> implements ProcessInstanceService {
}
