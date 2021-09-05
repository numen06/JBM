package com.jbm.cluster.center.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.model.entity.GatewayAccessLogs;
import com.jbm.cluster.center.mapper.GatewayLogsMapper;
import com.jbm.cluster.center.service.GatewayAccessLogsService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.masterdata.utils.ServiceUtils;
import com.jbm.framework.usage.paging.DataPaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author wesley.zhang
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GatewayAccessLogsServiceImpl implements GatewayAccessLogsService {

    @Autowired
    private GatewayLogsMapper gatewayLogsMapper;

    /**
     * 分页查询
     *
     * @param pageRequestBody
     * @return
     */
    @Override
    public DataPaging<GatewayAccessLogs> findListPage(PageRequestBody pageRequestBody) {
        GatewayAccessLogs query = pageRequestBody.tryGet(GatewayAccessLogs.class);
        QueryWrapper<GatewayAccessLogs> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .likeRight(ObjectUtils.isNotEmpty(query.getPath()), GatewayAccessLogs::getPath, query.getPath())
                .eq(ObjectUtils.isNotEmpty(query.getIp()), GatewayAccessLogs::getIp, query.getIp())
                .eq(ObjectUtils.isNotEmpty(query.getServiceId()), GatewayAccessLogs::getServiceId, query.getServiceId());
        queryWrapper.orderByDesc("request_time");
        IPage page = gatewayLogsMapper.selectPage(pageRequestBody.getPageParams(), queryWrapper);
        return ServiceUtils.pageToDataPaging(page);
    }
}
