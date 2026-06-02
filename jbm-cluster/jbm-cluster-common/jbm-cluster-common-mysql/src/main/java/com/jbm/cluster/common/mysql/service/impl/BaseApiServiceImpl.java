package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.constants.ResourceType;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.common.mysql.mapper.BaseApiMapper;
import com.jbm.cluster.common.mysql.service.BaseApiService;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.cluster.api.form.BaseApiForm;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author wesley.zhang
 */
@Slf4j
@Service
public class BaseApiServiceImpl extends MasterDataServiceImpl<BaseApi> implements BaseApiService {
    @Autowired
    private BaseApiMapper baseApiMapper;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;
    @Resource
    @Lazy
    private BaseApiService self;
    private static final int API_DESC_MAX_LENGTH = 100;

    @Override
    public BaseApi saveEntity(BaseApi baseApi) {
        if (baseApi == null) {
            throw new ServiceException("接口不能为空");
        }
        if (ObjectUtil.isEmpty(baseApi.getApiId())) {
            doAddApi(baseApi);
        } else {
            doUpdateApi(baseApi);
        }
        jbmClusterTemplate.refreshGateway();
        return baseApi;
    }

    private void normalizeApi(BaseApi api) {
        if (api != null && api.getApiDesc() != null && api.getApiDesc().length() > API_DESC_MAX_LENGTH) {
            api.setApiDesc(api.getApiDesc().substring(0, API_DESC_MAX_LENGTH));
        }
    }

    /**
     * 分页查询
     *
     * @param pageRequestBody
     * @return
     */
    @Override
    public DataPaging<BaseApi> findListPage(BaseApiForm form) {
        QueryWrapper<BaseApi> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .like(ObjectUtils.isNotEmpty(form.getPath()), BaseApi::getPath, form.getPath())
                .like(ObjectUtils.isNotEmpty(form.getApiName()), BaseApi::getApiName, form.getApiName())
                .like(ObjectUtils.isNotEmpty(form.getApiCode()), BaseApi::getApiCode, form.getApiCode())
                .eq(ObjectUtils.isNotEmpty(form.getServiceId()), BaseApi::getServiceId, form.getServiceId())
                .eq(ObjectUtils.isNotEmpty(form.getStatus()), BaseApi::getStatus, form.getStatus())
                .eq(ObjectUtils.isNotEmpty(form.getIsAuth()), BaseApi::getIsAuth, form.getIsAuth());
        queryWrapper.orderByDesc("create_time");
        PageForm pageForm = form.getPageForm() != null ? form.getPageForm() : new PageForm();
        return this.selectEntitys(PageParams.from(pageForm), queryWrapper);
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
        doAddApi(api);
    }

    private void doAddApi(BaseApi api) {
        normalizeApi(api);
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

    @Override
    public Integer batchUpdateOpen(List<String> ids, Boolean open) {
        QueryWrapper<BaseApi> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(BaseApi::getApiId, ids);
        BaseApi entity = new BaseApi();
        entity.setIsOpen(BooleanUtil.toInt(open));
        self.update(entity, wrapper);
        // 刷新网关
        jbmClusterTemplate.refreshGateway();
        return CollUtil.size(ids);
    }

    @Override
    public Integer batchUpdateAccessLog(List<String> ids, Boolean accessLog) {
        QueryWrapper<BaseApi> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(BaseApi::getApiId, ids);
        BaseApi entity = new BaseApi();
        entity.setAccessLog(accessLog);
        self.update(entity, wrapper);
        // 刷新网关
        jbmClusterTemplate.refreshGateway();
        return CollUtil.size(ids);
    }

    /**
     * 修改接口
     *
     * @param api
     * @return
     */
    @Override
    public void updateApi(BaseApi api) {
        doUpdateApi(api);
    }

    private void doUpdateApi(BaseApi api) {
        normalizeApi(api);
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
