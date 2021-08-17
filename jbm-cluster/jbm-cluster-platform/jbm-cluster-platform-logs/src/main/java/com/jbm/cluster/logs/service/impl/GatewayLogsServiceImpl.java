package com.jbm.cluster.logs.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.form.GatewayLogsForm;
import com.jbm.cluster.logs.repository.GatewayLogsRepository;
import com.jbm.cluster.logs.service.GatewayLogsService;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 16:56
 **/
@Service
public class GatewayLogsServiceImpl extends BaseDataServiceImpl<GatewayLogs, GatewayLogsRepository> implements GatewayLogsService {

    @Override
    public DataPaging<GatewayLogs> findLogs(GatewayLogsForm gatewayLogsForm) {
        Query query = new Query();
        query.addCriteria(this.likeCriteria("path", gatewayLogsForm.getGatewayLogs().getPath()));
        query.addCriteria(this.likeCriteria("region", gatewayLogsForm.getGatewayLogs().getRegion()));
        query.addCriteria(this.likeCriteria("serviceId", gatewayLogsForm.getGatewayLogs().getServiceId()));
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getBeginTime()))
            query.addCriteria(Criteria.where("requestTime").gte(gatewayLogsForm.getBeginTime()));
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getEndTime()))
            query.addCriteria(Criteria.where("requestTime").lte(gatewayLogsForm.getEndTime()));
//        List<Sort.Order> orders = new ArrayList<Sort.Order>();  //排序
//        orders.add(new Sort.Order(Sort.Direction.DESC, "requestTime"));
//        Sort sort = new Sort(orders);
        // 查询出一共的条数
        Long total = mongoTemplate.count(query, GatewayLogs.class);
        // 加上分页属性
        PageRequest pageable = this.toPageRequest(gatewayLogsForm.getPageForm(), Sort.Order.desc("requestTime"));
        query = query.with(pageable);

        // 查询
        List<GatewayLogs> list = mongoTemplate.find(query, GatewayLogs.class);
        // 将集合与分页结果封装
//        Page<GatewayLogs> pagelist = new PageImpl<GatewayLogs>(list, pageable, count);
        return new DataPaging<GatewayLogs>(list, total, gatewayLogsForm.getPageForm());
    }
}
