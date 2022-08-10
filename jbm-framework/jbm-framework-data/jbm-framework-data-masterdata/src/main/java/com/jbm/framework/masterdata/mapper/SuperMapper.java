package com.jbm.framework.masterdata.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jbm.framework.masterdata.usage.bean.EntityMap;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface SuperMapper<T> extends BaseMapper<T> {
    IPage<T> pageList(Page<T> page, @Param(Constants.WRAPPER) Wrapper<?> wrapper);


    List<EntityMap> getEntityMap(@Param(Constants.WRAPPER) Wrapper<?> wrapper);
}
