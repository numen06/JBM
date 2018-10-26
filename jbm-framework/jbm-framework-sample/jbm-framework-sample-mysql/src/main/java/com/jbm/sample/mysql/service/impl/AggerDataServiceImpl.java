package com.jbm.sample.mysql.service.impl;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.jbm.framework.metadata.exceptions.DataServiceException;
import com.jbm.framework.metadata.usage.page.PageForm;
import com.jbm.framework.sql.SuperSqlDaoImpl;
import com.jbm.sample.entity.AggregateData;
import com.jbm.sample.mysql.service.AggregateDataService;

/**
 * @Description: TODO
 * @author jim.xie
 * @date 2017年5月10日 下午2:19:19
 * @version version1.0
 */
@Service
public class AggerDataServiceImpl extends SuperSqlDaoImpl<AggregateData, String> implements AggregateDataService {

	@PostConstruct
	public void init() throws DataServiceException {
		// DataPaging<AggregateData> data =
		// this.selectMapperDataPaging("com.jbm.ev.program.mapper.DataMapper.getAggregateDataBetweenTime",
		// new PageForm());
		AggregateData entity = new AggregateData();
		entity.setDataType("total_flow");
		PageForm pageForm = new PageForm();
		pageForm.setCurrPage(1);
		pageForm.setPageSize(2);
		Map<String,Object> map = Maps.newHashMap();
		map.put("dataType", "cumulative_energy");
		System.out.println(JSON.toJSONString(this.selectEntitys(entity, pageForm)));
		System.out.println(JSON.toJSONString(this.selectEntitys(map, pageForm)));
	}

}
