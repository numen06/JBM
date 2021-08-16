package com.jbm.cluster.logs.repository;

import com.jbm.cluster.logs.entity.GatewayLogs;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 16:59
 **/
@Repository
public interface GatewayLogsRepository extends MongoRepository<GatewayLogs, String> {


}
