package com.jbm.cluster.logs.service.impl;

import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.repository.GatewayLogsRepository;
import com.jbm.cluster.logs.service.GatewayLogsService;
import org.springframework.stereotype.Service;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 16:56
 **/
@Service
public class GatewayLogsServiceImpl extends BaseDataServiceImpl<GatewayLogs, GatewayLogsRepository> implements GatewayLogsService {
}
