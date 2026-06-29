package com.jbm.framework.mvc.web;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.form.BaseRequsetBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 审计/日志类只读 Controller 基类，不提供增删改接口。
 */
@Slf4j
public abstract class AuditReadOnlyCollection<Entity extends MasterDataEntity, Service extends IMasterDataService<Entity>>
        extends BaseCollection {

    @Autowired
    protected Service service;

    protected void validator(BaseRequsetBody baseRequsetBody) throws RuntimeException {
        if (ObjectUtil.isNull(baseRequsetBody)) {
            throw new ServiceException("参数错误");
        }
    }

    protected Entity validatorMasterData(BaseRequsetBody baseRequsetBody, Boolean valNull) throws RuntimeException {
        Entity entity = baseRequsetBody.tryGet(service.currentEntityClass());
        if (valNull) {
            if (ObjectUtil.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
        }
        return entity;
    }

    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    public ResultBody<DataPaging<Entity>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            Entity entity = validatorMasterData(pageRequestBody, false);
            PageForm pageForm = pageRequestBody.getPageForm();
            DataPaging<Entity> dataPaging = service.selectEntitys(entity, pageForm);
            return ResultBody.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    public ResultBody<List<Entity>> list(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            Entity entity = validatorMasterData(masterDataRequsetBody, false);
            List<Entity> list = service.selectEntitys(entity);
            return ResultBody.success(list, "查询列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    public ResultBody<Entity> model(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            Entity entity = validatorMasterData(masterDataRequsetBody, true);
            entity = service.selectEntity(entity);
            return ResultBody.success(entity, "查询对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }
}
