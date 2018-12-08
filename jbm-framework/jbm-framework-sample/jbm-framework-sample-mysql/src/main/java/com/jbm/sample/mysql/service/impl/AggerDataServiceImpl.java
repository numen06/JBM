package com.jbm.sample.mysql.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.jbm.framework.masterdata.usage.paging.DataPaging;
import com.jbm.framework.masterdata.usage.paging.PageForm;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.sample.entity.AggregateData;
import com.jbm.sample.mysql.service.AggregateDataService;

/**
 * @Description: TODO
 * @date 2017年5月10日 下午2:19:19
 * @version version1.0
 */
@Service
public class AggerDataServiceImpl extends MasterDataServiceImpl<AggregateData> implements AggregateDataService {

	@Override
	public DataPaging<AggregateData> selectListByMapper(Map<String, Object> params, PageForm pageForm) {
		return this.selectMapperPaging("com.jbm.ev.program.mapper.DataMapper.getAggregateDataBetweenTime", params,
				pageForm);
	}

	@Override
	public DataPaging<Map<String, Object>> selectListByMapper2(Map<String, Object> params, PageForm pageForm) {
		return this.selectMapperPaging("com.jbm.ev.program.mapper.DataMapper.getAggregateDataBetweenTime", params,
				pageForm);
	}
//	@PostConstruct
//	public void init() throws DataServiceException {
//		// DataPaging<AggregateData> data =
//		// this.selectMapperDataPaging("com.jbm.ev.program.mapper.DataMapper.getAggregateDataBetweenTime",
//		// new PageForm());
//		AggregateData entity = new AggregateData();
//		entity.setDataType("total_flow");
//		entity.setData(1.0);
//		PageForm pageForm = new PageForm();
//		pageForm.setCurrPage(1);
//		pageForm.setPageSize(2);
//		Map<String, Object> map = Maps.newHashMap();
//		map.put("data_type", "cumulative_energy");
//		System.out.println(JSON.toJSONString(this.selectEntitys(entity, pageForm)));
//		System.out.println(JSON.toJSONString(this.selectEntitys(entity, map, pageForm)));
//	}

}
