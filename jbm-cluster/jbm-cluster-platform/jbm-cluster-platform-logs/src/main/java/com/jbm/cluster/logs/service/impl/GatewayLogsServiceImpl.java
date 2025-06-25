package com.jbm.cluster.logs.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.form.GatewayLogsForm;
import com.jbm.cluster.logs.service.GatewayLogsService;
import com.jbm.cluster.logs.tdengine.mapper.GatewayLogsMapper;
import com.jbm.framework.masterdata.utils.ServiceUtils;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.batch.BatchTask;
import jbm.framework.boot.autoconfigure.td.StableExecutor;
import jbm.framework.boot.autoconfigure.td.TdTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2021-05-06 16:56
 **/
@Service
@Slf4j
public class GatewayLogsServiceImpl extends ServiceImpl<GatewayLogsMapper, GatewayLogs> implements GatewayLogsService {


    @Resource
    private GatewayLogsMapper gatewayLogsMapper;


    @Override
    public DataPaging<GatewayLogs> findLogs(GatewayLogsForm gatewayLogsForm) {
        return this.findLogs(gatewayLogsForm, false);
    }

    @Override
    public DataPaging<GatewayLogs> findLogs(GatewayLogsForm gatewayLogsForm, Boolean isOperation) {
        IPage<GatewayLogs> page = ServiceUtils.buildPage(gatewayLogsForm.getPageForm());
        QueryWrapper<GatewayLogs> queryWrapper = new QueryWrapper<>(gatewayLogsForm.getGatewayLogs());
        List<GatewayLogs> list = gatewayLogsMapper.selectList(page, queryWrapper);
        // 查询
        return new DataPaging<>(list, page.getTotal(), page.getPages(), gatewayLogsForm.getPageForm());
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

    @Resource
    private TdTemplate tdTemplate;


    private final BatchTask<GatewayLogs> batchTask = new BatchTask<>(new Consumer<List<GatewayLogs>>() {
        @Override
        public void accept(List<GatewayLogs> gatewayLogs) {
//            gatewayLogsMapper.insert(gatewayLogs);
            saveBatch(gatewayLogs, 100);
        }
    });

    @Override
    public void saveGatewayLogs(GatewayLogs gatewayLogs) {
        try {
            StableExecutor executor = tdTemplate.getSTableExecutor(StrUtil.toUnderlineCase(GatewayLogs.class.getSimpleName()));
            executor.insertSubTable((g) -> {
                if (g.getApiId() == null) {
                    return "app_system";
                }
                return StrUtil.format("app_{}", g.getApiId());
            }, gatewayLogs);
        } catch (SQLException e) {
            log.error("保存日志失败", e);
        }
    }


}
