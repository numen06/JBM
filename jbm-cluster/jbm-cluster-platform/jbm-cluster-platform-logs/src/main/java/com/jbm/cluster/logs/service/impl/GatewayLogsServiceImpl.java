package com.jbm.cluster.logs.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.form.GatewayLogsForm;
import com.jbm.cluster.logs.service.GatewayLogsService;
import com.jbm.framework.masterdata.utils.ServiceUtils;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.batch.BatchTask;
import jbm.framework.boot.autoconfigure.openobserve.OpenObserveTemplate;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2021-05-06 16:56
 **/
@Service
@Slf4j
public class GatewayLogsServiceImpl implements GatewayLogsService {


    @Resource
    private OpenObserveTemplate openObserveTemplate;


    @Override
    public DataPaging<GatewayLogs> findOperationLogs(GatewayLogsForm gatewayLogsForm) {
        IPage<GatewayLogs> page = ServiceUtils.buildPage(gatewayLogsForm.getPageForm());
//        List<GatewayLogs> list = gatewayLogsMapper.selectList(page, queryWrapper);
        String statement = "com.jbm.cluster.logs.mapper.GatewayLogsMapper.selectOperationLogs";
        Map<String, Object> params = BeanUtil.beanToMap(gatewayLogsForm.getGatewayLogs());
        params.put("beginTime", gatewayLogsForm.getBeginTime());
        params.put("endTime", gatewayLogsForm.getEndTime());
        QueryResult queryResult = openObserveTemplate.selectLogs(statement, params, gatewayLogsForm.getBeginTime(), gatewayLogsForm.getEndTime(), gatewayLogsForm.getPageForm());
        List<Map<String, Object>> hits = queryResult.getHits();
        List<GatewayLogs> list = hits.stream().map(map -> {
            JSONObject jsonObject = new JSONObject(map);
            return jsonObject.toJavaObject(GatewayLogs.class);
        }).collect(Collectors.toList());
        // 查询
        return new DataPaging<>(list, queryResult.getScanRecords(), page.getPages(), gatewayLogsForm.getPageForm());
    }

    @Override
    public DataPaging<GatewayLogs> findLogs(GatewayLogsForm gatewayLogsForm) {
        IPage<GatewayLogs> page = ServiceUtils.buildPage(gatewayLogsForm.getPageForm());
//        List<GatewayLogs> list = gatewayLogsMapper.selectList(page, queryWrapper);
        String statement = "com.jbm.cluster.logs.mapper.GatewayLogsMapper.selectLogs";
        Map<String, Object> params = BeanUtil.beanToMap(gatewayLogsForm.getGatewayLogs());
        params.put("beginTime", gatewayLogsForm.getBeginTime());
        params.put("endTime", gatewayLogsForm.getEndTime());
        QueryResult queryResult = openObserveTemplate.selectLogs(statement, params, gatewayLogsForm.getBeginTime(), gatewayLogsForm.getEndTime(), gatewayLogsForm.getPageForm());
        List<Map<String, Object>> hits = queryResult.getHits();
        List<GatewayLogs> list = hits.stream().map(map -> {
            JSONObject jsonObject = new JSONObject(map);
            return jsonObject.toJavaObject(GatewayLogs.class);
        }).collect(Collectors.toList());
        // 查询
        return new DataPaging<>(list, queryResult.getScanRecords(), page.getPages(), gatewayLogsForm.getPageForm());
    }

    /**
     * @return
     */
    @Override
    public Long totalAccess() {
        return 0L;
    }

    /**
     * @return
     */
    @Override
    public Long todayAccess() {
        return 0L;
    }


    private final BatchTask<GatewayLogs> batchTask = new BatchTask<>(5L, TimeUnit.SECONDS, 100,new Consumer<List<GatewayLogs>>() {
        @Override
        public void accept(List<GatewayLogs> gatewayLogs) {
            openObserveTemplate.postLogs(gatewayLogs, GatewayLogs.class.getSimpleName());
//            log.info("保存日志成功数量:{}", gatewayLogs.size());
        }
    });

    @Override
    public void saveGatewayLogs(GatewayLogs gatewayLogs) {
        batchTask.offerBlocking(gatewayLogs);
    }


}
