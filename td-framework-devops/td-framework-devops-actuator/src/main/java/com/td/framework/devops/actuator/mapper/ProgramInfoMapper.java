package com.td.framework.devops.actuator.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.td.framework.devops.actuator.masterdata.entity.ProgramInfo;

@Mapper
public interface ProgramInfoMapper extends BaseMapper<ProgramInfo> {

}
