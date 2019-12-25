package com.jbm.cluster.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jbm.cluster.api.model.entity.BaseApi;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.usage.paging.PageForm;

import java.util.List;

/**
 * 接口资源管理
 *
 * @author liuyadu
 */
public interface BaseApiService extends IMasterDataService<BaseApi> {
    /**
     * 分页查询
     *
     * @param pageForm
     * @return
     */
    DataPaging<BaseApi> findListPage(PageForm pageForm);

    /**
     * 查询列表
     *
     * @return
     */
    List<BaseApi> findAllList(String serviceId);

    /**
     * 根据主键获取接口
     *
     * @param apiId
     * @return
     */
    BaseApi getApi(Long apiId);


    /**
     * 检查接口编码是否存在
     *
     * @param apiCode
     * @return
     */
    Boolean isExist(String apiCode);

    /**
     * 添加接口
     *
     * @param api
     * @return
     */
    void addApi(BaseApi api);

    /**
     * 修改接口
     *
     * @param api
     * @return
     */
    void updateApi(BaseApi api);

    /**
     * 查询接口
     *
     * @param apiCode
     * @return
     */
    BaseApi getApi(String apiCode);

    /**
     * 移除接口
     *
     * @param apiId
     * @return
     */
    void removeApi(Long apiId);


    /**
     * 获取数量
     *
     * @param queryWrapper
     * @return
     */
    int getCount(QueryWrapper<BaseApi> queryWrapper);

}
