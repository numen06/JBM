package com.jbm.cluster.bigscreen.service;

import com.jbm.cluster.api.entitys.bigscreen.BigscreenView;
import com.jbm.framework.masterdata.service.IMasterDataService;

/**
 * @Author: wesley.zhang
 * @Create: 2021-09-03 17:08:07
 */
public interface BigscreenViewService extends IMasterDataService<BigscreenView> {
    Boolean isUpload(BigscreenView bigscreenView);

    void cleanView(BigscreenView bigscreenView);

    boolean deleteById(Long id);

    BigscreenView upload(BigscreenView bigscreenView);
}
