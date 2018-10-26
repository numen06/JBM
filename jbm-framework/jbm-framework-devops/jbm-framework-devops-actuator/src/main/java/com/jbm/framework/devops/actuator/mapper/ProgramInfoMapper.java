package com.jbm.framework.devops.actuator.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.jbm.framework.devops.actuator.masterdata.entity.ProgramInfo;

@Mapper
public interface ProgramInfoMapper extends BaseMapper<ProgramInfo> {

}
