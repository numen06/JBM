package com.jbm.sample.rest;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.mvc.form.JsonRequestBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import com.jbm.sample.entity.AggregateData;
import com.jbm.sample.mysql.service.AggregateDataService;

@RestController
public class AggregateDataController extends MasterDataCollection<AggregateData, AggregateDataService> {

	@RequestMapping("/list")
	public Object list() {
		return this.service.selectAllEntitys();
	}

	@RequestMapping("/mapperList")
	public Object mapperList(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		if (jsonRequestBody == null)
			return this.service.selectListByMapper(null, null);
		return this.service.selectListByMapper(jsonRequestBody.getInnerMap(), jsonRequestBody.getPageForm());
	}

	@RequestMapping("/mapperList2")
	public Object mapperList2(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		return this.service.selectListByMapper2(jsonRequestBody.getInnerMap(), jsonRequestBody.getPageForm());
	}

	@RequestMapping("/selectEntityMapByCodes")
	public Object selectEntityMapByCodes(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		return ResultForm.success(this.service.selectEntityMapByCodes(jsonRequestBody.getList("codes", String.class)), "查询成功");
	}

}
