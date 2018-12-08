package com.jbm.sample.mysql.service;

import java.util.Map;

import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.paging.DataPaging;
import com.jbm.framework.masterdata.usage.paging.PageForm;
import com.jbm.sample.entity.AggregateData;

/**
 * @version version1.0
 * @Description: TODO
 * @date 2017年5月10日 下午2:18:32
 */
public interface AggregateDataService extends IMasterDataService<AggregateData> {

	DataPaging<AggregateData> selectListByMapper(Map<String, Object> params,PageForm pageForm);

	DataPaging<Map<String, Object>> selectListByMapper2(Map<String, Object> params, PageForm pageForm);

}
