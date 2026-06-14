package com.jbm.cluster.push.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.message.PushConfigInfo;
import com.jbm.cluster.push.service.PushConfigInfoService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2021-08-25 12:58:58
 */
@Api(tags = "推送配置设置")
@RestController
@RequestMapping("/pushConfigInfo")
public class PushConfigInfoController extends MasterDataCollection<PushConfigInfo, PushConfigInfoService> {

    private static final String[] EXISTING_COLUMNS = {
            "id", "enable", "type", "release_content", "create_time", "update_time"
    };

    @Override
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    public ResultBody<DataPaging<PushConfigInfo>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            PushConfigInfo entity = resolveQuery(pageRequestBody);
            QueryWrapper<PushConfigInfo> queryWrapper = buildQuery(entity);
            DataPaging<PushConfigInfo> dataPaging = service.selectEntitysByWapper(queryWrapper, resolvePageForm(pageRequestBody));
            return ResultBody.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @Override
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    public ResultBody<List<PushConfigInfo>> list(@RequestBody(required = false) com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody masterDataRequsetBody) {
        try {
            PushConfigInfo entity = resolveQuery(masterDataRequsetBody);
            List<PushConfigInfo> list = service.selectEntitysByWapper(buildQuery(entity));
            return ResultBody.success(list, "查询列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    private PushConfigInfo resolveQuery(com.jbm.framework.usage.form.BaseRequsetBody form) {
        if (form == null) {
            return new PushConfigInfo();
        }
        PushConfigInfo entity = form.tryGet(PushConfigInfo.class);
        return entity == null ? new PushConfigInfo() : entity;
    }

    private PageForm resolvePageForm(PageRequestBody form) {
        if (form == null || form.getPageForm() == null) {
            return new PageForm(1, 20);
        }
        return form.getPageForm();
    }

    private QueryWrapper<PushConfigInfo> buildQuery(PushConfigInfo entity) {
        QueryWrapper<PushConfigInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(EXISTING_COLUMNS);
        if (entity == null) {
            return queryWrapper;
        }
        if (ObjectUtil.isNotNull(entity.getId())) {
            queryWrapper.eq("id", entity.getId());
        }
        if (ObjectUtil.isNotNull(entity.getEnable())) {
            queryWrapper.eq("enable", entity.getEnable());
        }
        if (ObjectUtil.isNotNull(entity.getType())) {
            queryWrapper.eq("type", entity.getType());
        }
        return queryWrapper;
    }
}
