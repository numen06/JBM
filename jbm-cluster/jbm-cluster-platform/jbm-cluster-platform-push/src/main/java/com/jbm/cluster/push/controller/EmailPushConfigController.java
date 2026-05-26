package com.jbm.cluster.push.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.jsonzou.jmockdata.JMockData;
import com.github.jsonzou.jmockdata.MockConfig;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.form.IdsForm;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import com.jbm.framework.usage.form.EntityPageSearchForm;
import com.jbm.framework.usage.form.EntityRequestForm;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.cluster.api.entitys.push.EmailPushConfig;
import com.jbm.cluster.push.service.EmailPushConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2026-05-26 15:56:55
 */
@Api(tags = "消息推送项开放接口")
@RestController
@RequestMapping("/emailPushConfig")
public class EmailPushConfigController extends BaseController {

    @Resource
    private EmailPushConfigService service;

    private EmailPushConfig resolveQuery(EntityRequestForm<EmailPushConfig> form) {
        if (form == null || form.getEntity() == null) {
            return new EmailPushConfig();
        }
        return form.getEntity();
    }

    private PageForm resolvePageForm(EntityPageSearchForm<EmailPushConfig> form) {
        if (form == null || form.getPageForm() == null) {
            return new PageForm();
        }
        return form.getPageForm();
    }

    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    public ResultBody<DataPaging<EmailPushConfig>> pageList(
            @RequestBody(required = false) EntityPageSearchForm<EmailPushConfig> form) {
        try {
            EmailPushConfig entity = resolveQuery(form);
            PageForm pageForm = resolvePageForm(form);
            DataPaging<EmailPushConfig> dataPaging = service.selectEntitys(entity, pageForm);
            return ResultBody.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    public ResultBody<List<EmailPushConfig>> list(
            @RequestBody(required = false) EntityRequestForm<EmailPushConfig> form) {
        try {
            EmailPushConfig entity = resolveQuery(form);
            List<EmailPushConfig> list = service.selectEntitys(entity);
            return ResultBody.success(list, "查询列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    public ResultBody<EmailPushConfig> model(
            @RequestBody(required = false) EntityRequestForm<EmailPushConfig> form) {
        try {
            EmailPushConfig entity = resolveQuery(form);
            if (ObjectUtil.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
            entity = service.selectEntity(entity);
            return ResultBody.success(entity, "查询对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    public ResultBody<EmailPushConfig> save(
            @RequestBody(required = false) EntityRequestForm<EmailPushConfig> form) {
        try {
            EmailPushConfig entity = resolveQuery(form);
            if (ObjectUtil.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
            entity = service.saveEntity(entity);
            return ResultBody.success(entity, "保存对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    public ResultBody<List<EmailPushConfig>> saveBatch(
            @RequestBody(required = false) EntityRequestForm<EmailPushConfig> form) {
        try {
            List<EmailPushConfig> entitys = form != null ? form.tryGetList(service.currentEntityClass()) : null;
            if (CollectionUtil.isEmpty(entitys)) {
                throw new ServiceException("列表参数为空");
            }
            service.saveBatch(entitys);
            return ResultBody.success(entitys, "保存对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    public ResultBody<EmailPushConfig> mock() {
        try {
            MockConfig mockConfig = MockConfig.newInstance().setEnabledCircle(true).excludes("id").globalConfig();
            EmailPushConfig entity = JMockData.mock(service.currentEntityClass(), mockConfig);
            entity = service.saveEntity(entity);
            return ResultBody.success(entity, "保存对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "删除实体", notes = "删除实体")
    @PostMapping("/delete")
    public ResultBody<Boolean> remove(
            @RequestBody(required = false) EntityRequestForm<EmailPushConfig> form) {
        try {
            EmailPushConfig entity = resolveQuery(form);
            if (ObjectUtil.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
            if (service.deleteEntity(entity)) {
                return ResultBody.success(true, "删除对象成功");
            } else {
                return ResultBody.success(false, "删除对象失败");
            }
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "通过id删除实体", notes = "通过id删除实体")
    @PostMapping("/deleteByIds")
    public ResultBody<Boolean> deleteByIds(@RequestBody(required = false) IdsForm idsForm) {
        try {
            List<Long> ids = idsForm.getIds();
            if (CollectionUtil.isEmpty(ids)) {
                return ResultBody.error(true, "ID为空");
            }
            if (service.removeByIds(ids)) {
                return ResultBody.success(false, "批量成功刪除");
            }
            return ResultBody.error(false, "批量成功刪除");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }
}
