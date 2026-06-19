package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.constants.ResourceType;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.common.mysql.mapper.BaseApiMapper;
import com.jbm.cluster.common.mysql.mapper.BaseAuthorityApikeyMapper;
import com.jbm.cluster.common.mysql.mapper.BaseAuthorityMapper;
import com.jbm.cluster.common.mysql.mapper.GatewayIpLimitApiMapper;
import com.jbm.cluster.common.mysql.mapper.GatewayRateLimitApiMapper;
import com.jbm.cluster.common.mysql.service.BaseApiService;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.api.model.api.ApiControlCount;
import com.jbm.cluster.api.model.api.ApiControlSummary;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wesley.zhang
 */
@Slf4j
@Service
public class BaseApiServiceImpl extends MasterDataServiceImpl<BaseApi> implements BaseApiService {
    @Autowired
    private BaseApiMapper baseApiMapper;
    @Autowired
    private BaseAuthorityMapper baseAuthorityMapper;
    @Autowired
    private BaseAuthorityApikeyMapper baseAuthorityApikeyMapper;
    @Autowired
    private GatewayRateLimitApiMapper gatewayRateLimitApiMapper;
    @Autowired
    private GatewayIpLimitApiMapper gatewayIpLimitApiMapper;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;
    @Resource
    @Lazy
    private BaseApiService self;

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

    /**
     * 分页查询
     *
     * @param pageRequestBody
     * @return
     */
    @Override
    public DataPaging<BaseApi> findListPage(BaseApiForm form) {
        QueryWrapper<BaseApi> queryWrapper = new QueryWrapper();
        String keyword = form.getKeyword();
        queryWrapper.lambda()
                .like(ObjectUtils.isNotEmpty(form.getPath()), BaseApi::getPath, form.getPath())
                .like(ObjectUtils.isNotEmpty(form.getApiName()), BaseApi::getApiName, form.getApiName())
                .like(ObjectUtils.isNotEmpty(form.getApiCode()), BaseApi::getApiCode, form.getApiCode())
                .eq(StrUtil.isNotBlank(form.getRequestMethod()), BaseApi::getRequestMethod, StrUtil.blankToDefault(form.getRequestMethod(), "").toUpperCase())
                .eq(ObjectUtils.isNotEmpty(form.getServiceId()), BaseApi::getServiceId, form.getServiceId())
                .eq(ObjectUtils.isNotEmpty(form.getStatus()), BaseApi::getStatus, form.getStatus())
                .eq(form.getIsOpen() != null, BaseApi::getIsOpen, form.getIsOpen())
                .eq(form.getIsAuth() != null, BaseApi::getIsAuth, form.getIsAuth())
                .eq(form.getAccessLog() != null, BaseApi::getAccessLog, form.getAccessLog());
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.lambda().and(w -> w
                    .like(BaseApi::getApiCode, keyword)
                    .or().like(BaseApi::getApiName, keyword)
                    .or().like(BaseApi::getPath, keyword)
                    .or().like(BaseApi::getServiceId, keyword));
        }
        queryWrapper.orderByDesc("create_time");
        PageForm pageForm = form.getPageForm() != null ? form.getPageForm() : new PageForm();
        DataPaging<BaseApi> paging = this.selectEntitys(PageParams.from(pageForm), queryWrapper);
        fillControlSummary(paging.getContents());
        return paging;
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
        fillControlSummary(list);
        return list;
    }

    @Override
    public List<String> findServiceIds() {
        QueryWrapper<BaseApi> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("service_id");
        queryWrapper.isNotNull("service_id");
        queryWrapper.groupBy("service_id");
        queryWrapper.orderByAsc("service_id");
        return baseApiMapper.selectList(queryWrapper).stream()
                .map(BaseApi::getServiceId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    /**
     * 根据主键获取接口
     *
     * @param apiId
     * @return
     */
    @Override
    public BaseApi getApi(Long apiId) {
        BaseApi api = baseApiMapper.selectById(apiId);
        fillControlSummary(api == null ? Collections.emptyList() : Collections.singletonList(api));
        return api;
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
        fillControlSummary(list);
        return CollUtil.getFirst(list);
    }

    @Override
    public BaseApi findApiByServicePathMethod(String serviceId, String path, String requestMethod) {
        if (ObjectUtils.isEmpty(serviceId) || ObjectUtils.isEmpty(path) || ObjectUtils.isEmpty(requestMethod)) {
            return null;
        }
        QueryWrapper<BaseApi> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .eq(BaseApi::getServiceId, serviceId)
                .eq(BaseApi::getPath, path)
                .eq(BaseApi::getRequestMethod, requestMethod.toUpperCase());
        List<BaseApi> list = baseApiMapper.selectList(queryWrapper);
        fillControlSummary(list);
        return CollUtil.getFirst(list);
    }

    @Override
    public void fillControlSummary(List<BaseApi> apis) {
        if (CollUtil.isEmpty(apis)) {
            return;
        }
        List<Long> apiIds = apis.stream()
                .map(BaseApi::getApiId)
                .filter(ObjectUtil::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(apiIds)) {
            return;
        }
        Map<Long, Long> authorityCounts = toCountMap(baseAuthorityMapper.countAuthorityByApiIds(apiIds));
        Map<Long, Long> apiKeyGrantCounts = toCountMap(baseAuthorityApikeyMapper.countApiKeyGrantByApiIds(apiIds));
        Map<Long, Long> rateLimitCounts = toCountMap(gatewayRateLimitApiMapper.countRateLimitByApiIds(apiIds));
        Map<Long, Long> ipLimitCounts = toCountMap(gatewayIpLimitApiMapper.countIpLimitByApiIds(apiIds));

        for (BaseApi api : apis) {
            Long apiId = api.getApiId();
            ApiControlSummary summary = new ApiControlSummary();
            boolean open = api.getIsOpen() != null && api.getIsOpen() == 1;
            boolean auth = BooleanUtil.isTrue(api.getIsAuth());
            long authorityCount = authorityCounts.getOrDefault(apiId, 0L);
            long apiKeyGrantCount = apiKeyGrantCounts.getOrDefault(apiId, 0L);
            long rateLimitCount = rateLimitCounts.getOrDefault(apiId, 0L);
            long ipLimitCount = ipLimitCounts.getOrDefault(apiId, 0L);
            summary.setVisibility(open ? "external" : "internal");
            summary.setAuthentication(auth ? "required" : "anonymous");
            summary.setAuthorityCount(authorityCount);
            summary.setApiKeyGrantCount(apiKeyGrantCount);
            summary.setRateLimitPolicyCount(rateLimitCount);
            summary.setIpLimitPolicyCount(ipLimitCount);
            summary.setExternallyControlled(open && (auth || apiKeyGrantCount > 0 || rateLimitCount > 0 || ipLimitCount > 0));
            summary.setInternallyControlled(!open && (auth || authorityCount > 0));
            summary.setControlMode(resolveControlMode(open, auth, apiKeyGrantCount, rateLimitCount, ipLimitCount));
            api.setControlSummary(summary);
        }
    }

    private static Map<Long, Long> toCountMap(List<ApiControlCount> counts) {
        if (CollUtil.isEmpty(counts)) {
            return Collections.emptyMap();
        }
        return counts.stream()
                .filter(item -> item.getApiId() != null)
                .collect(Collectors.toMap(ApiControlCount::getApiId,
                        item -> item.getCount() == null ? 0L : item.getCount(),
                        Long::sum));
    }

    private static String resolveControlMode(boolean open, boolean auth, long apiKeyGrantCount,
                                             long rateLimitCount, long ipLimitCount) {
        if (open) {
            if (apiKeyGrantCount > 0) {
                return "external_api_key";
            }
            if (auth) {
                return "external_authenticated";
            }
            if (rateLimitCount > 0 || ipLimitCount > 0) {
                return "external_guarded";
            }
            return "external_public";
        }
        if (auth) {
            return "internal_authenticated";
        }
        return "internal_service";
    }


}
