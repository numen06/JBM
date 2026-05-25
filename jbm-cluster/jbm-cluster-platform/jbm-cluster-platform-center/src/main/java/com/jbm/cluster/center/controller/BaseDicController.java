package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.cluster.common.mysql.service.BaseDicService;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataTreeCollection;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:47:52
 */
@Api(tags = "系统字典")
@RestController
@RequestMapping("/baseDic")
public class BaseDicController extends MasterDataTreeCollection<BaseDic, BaseDicService> {

    @ApiOperation("获取数据字典")
    @GetMapping("/getDicMap")
    public ResultBody<Map<String, List<BaseDic>>> getDicMap() {
        Map<String, List<BaseDic>> result = this.service.getDicMap();
        return ResultBody.success(result, "获取数据字典成功");
    }

    @ApiOperation("分页查询字典分组（根节点）")
    @PostMapping("/root/pageList")
    public ResultBody<DataPaging<BaseDic>> rootPageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            BaseDic filter = validatorMasterData(pageRequestBody, false);
            PageForm pageForm = pageRequestBody.getPageForm();
            String keyword = resolveKeyword(filter);
            DataPaging<BaseDic> dataPaging = this.service.pageRootList(keyword, pageForm);
            return ResultBody.success(dataPaging, "查询字典分组成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation("分页查询分组下的字典项")
    @PostMapping("/items/pageList")
    public ResultBody<DataPaging<BaseDic>> itemsPageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            BaseDic filter = validatorMasterData(pageRequestBody, true);
            PageForm pageForm = pageRequestBody.getPageForm();
            String keyword = resolveKeyword(filter);
            DataPaging<BaseDic> dataPaging = this.service.pageItemsByParentId(filter.getParentId(), keyword, pageForm);
            return ResultBody.success(dataPaging, "查询字典项成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    private static String resolveKeyword(BaseDic filter) {
        if (filter == null) {
            return null;
        }
        if (StrUtil.isNotBlank(filter.getName())) {
            return filter.getName().trim();
        }
        if (StrUtil.isNotBlank(filter.getCode())) {
            return filter.getCode().trim();
        }
        if (StrUtil.isNotBlank(filter.getRemark())) {
            return filter.getRemark().trim();
        }
        return null;
    }


}
