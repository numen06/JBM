package com.jbm.cluster.center.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.form.BaseRoleForm;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Repository
public interface BaseRoleMapper extends SuperMapper<BaseRole> {

    /**
     * 查询列表
     *
     * @param form
     * @return
     */
    List<BaseRole> selectData(@Param("form") BaseRoleForm form);

    /**
     * 查询分页列表
     *
     * @param form
     * @param page
     * @return
     */
    IPage<BaseRole> selectData(@Param("form") BaseRoleForm form, Page page);
}
