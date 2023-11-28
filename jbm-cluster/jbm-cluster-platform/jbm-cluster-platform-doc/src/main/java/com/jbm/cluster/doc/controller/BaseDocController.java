package com.jbm.cluster.doc.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.doc.service.BaseDocService;
import com.jbm.framework.form.ObjectIdsForm;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2022-07-20 14:46:37
 */
@Api(tags = "文档管理开放接口")
@RestController
@RequestMapping("/baseDoc")
public class BaseDocController {

    @Autowired
    private BaseDocService baseDocService;

    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    public ResultBody<DataPaging<BaseDoc>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        BaseDoc entity = pageRequestBody.tryGet(baseDocService.currentEntityClass());
        PageForm pageForm = pageRequestBody.getPageForm();
        DataPaging<BaseDoc> dataPaging = baseDocService.selectEntitys(entity, pageForm);
        return ResultBody.success(dataPaging, "查询分页列表成功");
    }

    @ApiOperation(value = "通过id删除实体", notes = "通过id删除实体")
    @PostMapping("/deleteByIds")
    public ResultBody<Boolean> deleteByIds(@RequestBody(required = false) ObjectIdsForm idsForm) {
        try {
            // 获取前端信息List<BusCompanyInfo>
            List<String> ids = idsForm.getIds();
            if (CollectionUtil.isEmpty(ids)) {
                return ResultBody.error(true, "ID为空");
            }
            if (this.baseDocService.removeByIds(ids)) {
                return ResultBody.success(false, "批量成功刪除");
            }
            return ResultBody.error(false, "批量成功刪除");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }



}
