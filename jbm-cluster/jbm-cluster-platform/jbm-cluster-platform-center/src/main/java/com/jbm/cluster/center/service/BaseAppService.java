package com.jbm.cluster.center.service;

import com.jbm.cluster.api.model.entity.BaseApp;
import com.jbm.cluster.common.security.OpenClientDetails;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;

/**
 * 应用信息管理
 *
 * @author wesley.zhang
 */
public interface BaseAppService extends IMasterDataService<BaseApp> {

    /**
     * 查询应用列表
     *
     * @param pageRequestBody
     * @return
     */
    DataPaging<BaseApp> findListPage(PageRequestBody<BaseApp> pageRequestBody);

    /**
     * 获取app信息
     *
     * @param appId
     * @return
     */
    BaseApp getAppInfo(String appId);

    /**
     * 获取app和应用信息
     *
     * @param clientId
     * @return
     */
    OpenClientDetails getAppClientInfo(String clientId);


    /**
     * 更新应用开发新型
     *
     * @param client
     */
    void updateAppClientInfo(OpenClientDetails client);

    /**
     * 添加应用
     *
     * @param app 应用
     * @return 应用信息
     */
    BaseApp addAppInfo(BaseApp app);

    /**
     * 修改应用
     *
     * @param app 应用
     * @return 应用信息
     */
    BaseApp updateInfo(BaseApp app);


    /**
     * 重置秘钥
     *
     * @param appId
     * @return
     */
    String restSecret(String appId);

    /**
     * 删除应用
     *
     * @param appId
     * @return
     */
    void removeApp(String appId);
}
