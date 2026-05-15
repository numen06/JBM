package com.jbm.micro.mysql.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 从库连通性探测（从库独立 H2 库，无业务表，仅验证多数据源路由）。
 */
@Mapper
public interface SlaveProbeMapper {

    @DS("slave")
    @Select("SELECT 1")
    Integer ping();
}
