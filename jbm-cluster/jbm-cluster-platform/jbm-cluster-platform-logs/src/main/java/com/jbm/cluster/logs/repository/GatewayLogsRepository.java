package com.jbm.cluster.logs.repository;

import com.jbm.cluster.logs.entity.GatewayLogs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 16:59
 **/
@Repository
public interface GatewayLogsRepository extends ElasticsearchRepository<GatewayLogs, String> {


}
