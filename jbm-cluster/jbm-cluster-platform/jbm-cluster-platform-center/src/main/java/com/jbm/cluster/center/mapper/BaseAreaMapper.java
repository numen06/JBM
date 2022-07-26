package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.entitys.basic.BaseArea;
import com.jbm.framework.masterdata.annotation.MapperRepository;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2022-04-07 21:44:18
 */
@MapperRepository
public interface BaseAreaMapper extends SuperMapper<BaseArea> {

    /**
     * 根据 code 查询
     */
    BaseArea selectByCode(String code);

    @Select("<script>" +
            " select * " +
            " from base_area " +
            " where parent_code = #{parentCode}" +
            " </script>")
    List<BaseArea> selectByParentCode(String parentCode);
}
