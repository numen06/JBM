package com.jbm.cluster.logs.tdengine.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.framework.masterdata.annotation.MapperRepository;


@MapperRepository
//@DS(TD_DATASOURCE)
@InterceptorIgnore(tenantLine = "true")
public interface GatewayLogsMapper extends BaseMapper<GatewayLogs> {

}
