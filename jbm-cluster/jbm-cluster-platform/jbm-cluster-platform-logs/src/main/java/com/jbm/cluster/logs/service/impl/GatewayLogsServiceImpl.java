package com.jbm.cluster.logs.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.form.GatewayLogsForm;
import com.jbm.cluster.logs.repository.GatewayLogsRepository;
import com.jbm.cluster.logs.service.GatewayLogsService;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.batch.BatchTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.influx.SimpleInfluxTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2021-05-06 16:56
 **/
@Service
public class GatewayLogsServiceImpl extends BaseDataServiceImpl<GatewayLogs, GatewayLogsRepository> implements GatewayLogsService {
    @Override
    public DataPaging<GatewayLogs> findLogs(GatewayLogsForm gatewayLogsForm) {
        return this.findLogs(gatewayLogsForm, false);
    }

    @Override
    public DataPaging<GatewayLogs> findLogs(GatewayLogsForm gatewayLogsForm,Boolean isOperation) {

        // 查询
        DataPaging<GatewayLogs> dataPaging = simpleInfluxTemplate.selectPageList("select_logs", gatewayLogsForm.getPageForm(), GatewayLogs.class, gatewayLogsForm);


        return dataPaging;
    }

//    @Override
//    public DataPaging<GatewayLogs> findLogs(GatewayLogsForm gatewayLogsForm, Boolean isOperation) {
//        Query query = this.buildQuery(gatewayLogsForm, isOperation);
////        List<Sort.Order> orders = new ArrayList<Sort.Order>();  //排序
////        orders.add(new Sort.Order(Sort.Direction.DESC, "requestTime"));
////        Sort sort = new Sort(orders);
//        // 查询出一共的条数
//        Long total = mongoTemplate.count(query, GatewayLogs.class);
//        // 加上分页属性
//        PageRequest pageable = this.toPageRequest(gatewayLogsForm.getPageForm(), Sort.Order.desc("requestTime"));
//        query = query.with(pageable);
//
//        // 查询
//        List<GatewayLogs> list = mongoTemplate.find(query, GatewayLogs.class);
//        // 将集合与分页结果封装
////        Page<GatewayLogs> pagelist = new PageImpl<GatewayLogs>(list, ageable, count);
//        return new DataPaging<GatewayLogs>(list, total, gatewayLogsForm.getPageForm());
//    }

    @Override
    public Long totalAccess() {
        GatewayLogsForm gatewayLogsForm = new GatewayLogsForm();
        gatewayLogsForm.setGatewayLogs(new GatewayLogs());
        Query query = this.buildQuery(gatewayLogsForm, false);
        Long total = mongoTemplate.count(query, GatewayLogs.class);
        return total;
    }

    @Override
    public Long todayAccess() {
        GatewayLogsForm gatewayLogsForm = new GatewayLogsForm();
        gatewayLogsForm.setGatewayLogs(new GatewayLogs());
        Date now = DateTime.now();
        Date today = DateUtil.beginOfDay(now);
        gatewayLogsForm.setBeginTime(today);
        gatewayLogsForm.setBeginTime(DateUtil.offsetDay(today, 1));
        Query query = this.buildQuery(gatewayLogsForm, false);
        Long total = mongoTemplate.count(query, GatewayLogs.class);
        return total;
    }

    public Query buildQuery(GatewayLogsForm gatewayLogsForm, Boolean isOperation) {
        Query query = new Query();
        if (isOperation && StrUtil.isBlank(gatewayLogsForm.getGatewayLogs().getApiName())) {
            query.addCriteria(Criteria.where("apiName").exists(true));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getApiName())) {
            query.addCriteria(this.likeCriteria("apiName", gatewayLogsForm.getGatewayLogs().getApiName()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getPath())) {
            query.addCriteria(this.likeCriteria("path", gatewayLogsForm.getGatewayLogs().getPath()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getIp())) {
            query.addCriteria(this.likeCriteria("ip", gatewayLogsForm.getGatewayLogs().getIp()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getServiceId())) {
            query.addCriteria(this.likeCriteria("serviceId", gatewayLogsForm.getGatewayLogs().getServiceId()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getError())) {
            query.addCriteria(this.likeCriteria("error", gatewayLogsForm.getGatewayLogs().getError()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getHttpStatus())) {
            query.addCriteria(Criteria.where("httpStatus").is(gatewayLogsForm.getGatewayLogs().getHttpStatus()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getUseTime())) {
            query.addCriteria(Criteria.where("useTime").gte(gatewayLogsForm.getGatewayLogs().getUseTime()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getMethod())) {
            query.addCriteria(Criteria.where("method").is(gatewayLogsForm.getGatewayLogs().getMethod()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getOperationType())) {
            query.addCriteria(Criteria.where("operationType").is(gatewayLogsForm.getGatewayLogs().getOperationType()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getRequestUserId())) {
            query.addCriteria(Criteria.where("requestUserId").is(gatewayLogsForm.getGatewayLogs().getRequestUserId()));
        }
        if (ObjectUtil.isNotEmpty(gatewayLogsForm.getGatewayLogs().getRequestRealName())) {
            query.addCriteria(Criteria.where("requestRealName").is(gatewayLogsForm.getGatewayLogs().getRequestRealName()));
        }
        if (ObjectUtil.isAllNotEmpty(gatewayLogsForm.getBeginTime(), gatewayLogsForm.getEndTime())) {
            query.addCriteria(Criteria.where("requestTime").gte(gatewayLogsForm.getBeginTime()).lte(gatewayLogsForm.getEndTime()));
        } else {
            if (ObjectUtil.isNotEmpty(gatewayLogsForm.getBeginTime())) {
                query.addCriteria(Criteria.where("requestTime").gte(gatewayLogsForm.getBeginTime()));
            }
            if (ObjectUtil.isNotEmpty(gatewayLogsForm.getEndTime())) {
                query.addCriteria(Criteria.where("requestTime").lte(gatewayLogsForm.getEndTime()));
            }
        }
        return query;
    }


    @Autowired
    private SimpleInfluxTemplate simpleInfluxTemplate;

    private BatchTask<GatewayLogs> batchTask = new BatchTask<>(new Consumer<List<GatewayLogs>>() {
        @Override
        public void accept(List<GatewayLogs> gatewayLogs) {
            simpleInfluxTemplate.batchInsertItem("gatewayLogs", gatewayLogs, "requestTime", ListUtil.list(false, "requestUserId", "appId", "appKey", "serviceId"));
        }
    });

    @Override
    public void saveGatewayLogs(GatewayLogs gatewayLogs) {
        batchTask.offer(gatewayLogs);
    }


}
