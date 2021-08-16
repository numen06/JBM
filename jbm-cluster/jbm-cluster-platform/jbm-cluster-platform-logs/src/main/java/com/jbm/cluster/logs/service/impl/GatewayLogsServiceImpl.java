package com.jbm.cluster.logs.service.impl;

import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.repository.GatewayLogsRepository;
import com.jbm.cluster.logs.service.GatewayLogsService;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 16:56
 **/
@Service
public class GatewayLogsServiceImpl extends BaseDataServiceImpl<GatewayLogs, GatewayLogsRepository> implements GatewayLogsService {

    @Override
    public DataPaging<GatewayLogs> findLogs(PageForm pageForm, GatewayLogs gatewayLogs) {
        Query query = new Query();
//        List<Sort.Order> orders = new ArrayList<Sort.Order>();  //排序
//        orders.add(new Sort.Order(Sort.Direction.DESC, "requestTime"));
//        Sort sort = new Sort(orders);
        PageRequest pageable = new PageRequest(pageForm.getCurrPage(), pageForm.getPageSize());
        // 查询出一共的条数
        Long total = mongoTemplate.count(query, GatewayLogs.class);
        // 查询
        List<GatewayLogs> list = mongoTemplate.find(query.with(pageable), GatewayLogs.class);
        // 将集合与分页结果封装
//        Page<GatewayLogs> pagelist = new PageImpl<GatewayLogs>(list, pageable, count);
        return new DataPaging<GatewayLogs>(list, total, pageForm);
    }
}
