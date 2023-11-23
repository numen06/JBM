package com.jbm.cluster.center.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.constants.ResourceType;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.center.mapper.BaseApiMapper;
import com.jbm.cluster.center.service.BaseApiService;
import com.jbm.cluster.center.service.BaseAuthorityService;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author wesley.zhang
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseApiServiceImpl extends MasterDataServiceImpl<BaseApi> implements BaseApiService {
    @Autowired
    private BaseApiMapper baseApiMapper;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    @Override
    public BaseApi saveEntity(BaseApi baseApi) {
        if (ObjectUtil.isEmpty(baseApi)) {
            this.addApi(baseApi);
        } else {
            this.updateApi(baseApi);
        }
        jbmClusterTemplate.refreshGateway();
        return baseApi;
    }

    /**
     * 分页查询
     *
     * @param pageRequestBody
     * @return
     */
    @Override
    public DataPaging<BaseApi> findListPage(PageRequestBody pageRequestBody) {
        BaseApi query = pageRequestBody.tryGet(BaseApi.class);
        QueryWrapper<BaseApi> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .likeRight(ObjectUtils.isNotEmpty(query.getPath()), BaseApi::getPath, query.getPath())
                .likeRight(ObjectUtils.isNotEmpty(query.getApiName()), BaseApi::getApiName, query.getApiName())
                .likeRight(ObjectUtils.isNotEmpty(query.getApiCode()), BaseApi::getApiCode, query.getApiCode())
                .eq(ObjectUtils.isNotEmpty(query.getServiceId()), BaseApi::getServiceId, query.getServiceId())
                .eq(ObjectUtils.isNotEmpty(query.getStatus()), BaseApi::getStatus, query.getStatus())
                .eq(ObjectUtils.isNotEmpty(query.getIsAuth()), BaseApi::getIsAuth, query.getIsAuth());
        queryWrapper.orderByDesc("create_time");
        return this.selectEntitys(pageRequestBody.getPageParams(), queryWrapper);
    }

    /**
     * 查询列表
     *
     * @return
     */
    @Override
    public List<BaseApi> findAllList(String serviceId) {
        QueryWrapper<BaseApi> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(ObjectUtils.isNotEmpty(serviceId), BaseApi::getServiceId, serviceId);
        List<BaseApi> list = baseApiMapper.selectList(queryWrapper);
        return list;
    }

    /**
     * 根据主键获取接口
     *
     * @param apiId
     * @return
     */
    @Override
    public BaseApi getApi(Long apiId) {
        return baseApiMapper.selectById(apiId);
    }


    /**
     * 检查接口编码是否存在
     *
     * @param apiCode
     * @return
     */
    @Override
    public Boolean isExist(String apiCode) {
        QueryWrapper<BaseApi> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseApi::getApiCode, apiCode);
        Long count = getCount(queryWrapper);
        return count > 0 ? true : false;
    }

    /**
     * 添加接口
     *
     * @param api
     * @return
     */
    @Override
    public void addApi(BaseApi api) {
        //默认记入日志
        api.setAccessLog(ObjectUtil.defaultIfNull(api.getAccessLog(), true));
        if (isExist(api.getApiCode())) {
            throw new ServiceException(String.format("%s编码已存在!", api.getApiCode()));
        }
        if (api.getPriority() == null) {
            api.setPriority(0);
        }
        if (api.getStatus() == null) {
            api.setStatus(JbmConstants.ENABLED);
        }
        if (api.getApiCategory() == null) {
            api.setApiCategory(JbmConstants.DEFAULT_API_CATEGORY);
        }
        if (api.getIsPersist() == null) {
            api.setIsPersist(false);
        }
        if (api.getIsAuth() == null) {
            api.setIsAuth(false);
        }
        api.setCreateTime(new Date());
        api.setUpdateTime(api.getCreateTime());
        baseApiMapper.insert(api);
        // 同步权限表里的信息
        baseAuthorityService.saveOrUpdateAuthority(api.getApiId(), ResourceType.api);
    }

    /**
     * 修改接口
     *
     * @param api
     * @return
     */
    @Override
    public void updateApi(BaseApi api) {
        BaseApi saved = getApi(api.getApiId());
        saved.setAccessLog(ObjectUtil.defaultIfNull(saved.getAccessLog(), true));
        if (saved == null) {
            throw new ServiceException("信息不存在!");
        }
        if (!saved.getApiCode().equals(api.getApiCode())) {
            // 和原来不一致重新检查唯一性
            if (isExist(api.getApiCode())) {
                throw new ServiceException(String.format("%s编码已存在!", api.getApiCode()));
            }
        }
        if (api.getPriority() == null) {
            api.setPriority(0);
        }
        if (api.getApiCategory() == null) {
            api.setApiCategory(JbmConstants.DEFAULT_API_CATEGORY);
        }
        api.setUpdateTime(new Date());
        baseApiMapper.updateById(api);
        // 同步权限表里的信息
        baseAuthorityService.saveOrUpdateAuthority(api.getApiId(), ResourceType.api);
    }

    /**
     * 查询接口
     *
     * @param apiCode
     * @return
     */
    @Override
    public BaseApi getApi(String apiCode) {
        QueryWrapper<BaseApi> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseApi::getApiCode, apiCode);
        return baseApiMapper.selectOne(queryWrapper);
    }


    /**
     * 移除接口
     *
     * @param apiId
     * @return
     */
    @Override
    public void removeApi(Long apiId) {
        BaseApi api = getApi(apiId);
        if (api != null && api.getIsPersist().equals(JbmConstants.ENABLED)) {
            throw new ServiceException(String.format("保留数据,不允许删除"));
        }
        baseAuthorityService.removeAuthority(apiId, ResourceType.api);
        baseApiMapper.deleteById(apiId);
    }


    /**
     * 获取数量
     *
     * @param queryWrapper
     * @return
     */
    @Override
    public Long getCount(QueryWrapper<BaseApi> queryWrapper) {
        return baseApiMapper.selectCount(queryWrapper);
    }

    @Override
    public BaseApi findApiByPath(String serviceId, String path) {
        QueryWrapper<BaseApi> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseApi::getServiceId, serviceId);
        queryWrapper.lambda().eq(BaseApi::getPath, path);
        List<BaseApi> list = baseApiMapper.selectList(queryWrapper);
        return CollUtil.getFirst(list);
    }


}
