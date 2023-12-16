package com.jbm.framework.mvc.web;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.jsonzou.jmockdata.JMockData;
import com.github.jsonzou.jmockdata.MockConfig;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.form.ObjectIdsForm;
import com.jbm.framework.masterdata.controller.IMultiPlatformController;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformEntity;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.form.EntityPageSearchForm;
import com.jbm.framework.usage.form.EntityRequsetForm;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Slf4j
public abstract class MultiPlatformCollection<Entity extends MultiPlatformEntity, Service extends IMasterDataService<Entity>>
        extends BaseCollection implements IMultiPlatformController<Entity> {
    @Autowired
    protected Service service;
    @Autowired
    protected MessageSource messageSource;

    public MultiPlatformCollection() {
        super();
    }


    protected void validator(EntityRequsetForm<Entity> entityRequsetForm) throws Exception {
        if (ObjectUtil.isNull(entityRequsetForm)) {
            throw new ServiceException("参数错误");
        }

    }

    protected Entity validatorMasterData(EntityRequsetForm<Entity> entityRequsetForm, Boolean valNull) throws Exception {
        Entity entity = entityRequsetForm.tryGet(service.currentEntityClass());
        if (valNull) {
            if (ObjectUtil.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
        }
        return entity;
    }

    protected List<Entity> validatorMasterDataList(EntityRequsetForm<Entity> entityRequsetForm, Boolean valNull) throws Exception {
        List<Entity> entitys = entityRequsetForm.tryGetList(service.currentEntityClass());
        if (valNull) {
            if (CollectionUtil.isEmpty(entitys)) {
                throw new ServiceException("列表参数为空");
            }
            if (ObjectUtil.isNull(entitys)) {
                throw new ServiceException("参数错误");
            }
        }
        return entitys;
    }


    /**
     * 分页列表查询
     *
     * @param entityPageSearchForm
     * @return
     */
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    @Override
    public ResultBody<DataPaging<Entity>> pageList(@RequestBody(required = false) EntityPageSearchForm<Entity> entityPageSearchForm) {
        try {
            validator(entityPageSearchForm);
            Entity entity = validatorMasterData(entityPageSearchForm, false);
            PageForm pageForm = entityPageSearchForm.getPageForm();
            DataPaging<Entity> dataPaging = service.selectEntitys(entity, pageForm);
            return ResultBody.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    /**
     * 列表查询
     *
     * @param entityRequsetForm
     * @return
     */
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    @Override
    public ResultBody<List<Entity>> list(@RequestBody(required = false) EntityRequsetForm<Entity> entityRequsetForm) {
        try {
            validator(entityRequsetForm);
            Entity entity = validatorMasterData(entityRequsetForm, false);
            List<Entity> list = service.selectEntitys(entity);
            return ResultBody.success(list, "查询列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    /**
     * 查询实体
     *
     * @param entityRequsetForm
     * @return
     */
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
    public ResultBody<Entity> model(@RequestBody(required = false) EntityRequsetForm<Entity> entityRequsetForm) {
        try {
            validator(entityRequsetForm);
            Entity entity = validatorMasterData(entityRequsetForm, true);
            entity = service.selectEntity(entity);
            return ResultBody.success(entity, "查询对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }



    /**
     * 保存
     *
     * @param entityRequsetForm
     * @return
     */
    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    @Override
    public ResultBody<Entity> save(@RequestBody(required = false) EntityRequsetForm<Entity> entityRequsetForm) {
        try {
            validator(entityRequsetForm);
            Entity entity = validatorMasterData(entityRequsetForm, true);
            entity = service.saveEntity(entity);
            return ResultBody.success(entity, "保存对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    /**
     * 保存多个对象
     *
     * @param entityRequsetForm
     * @return
     */
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<Entity>> saveBatch(@RequestBody(required = false) EntityRequsetForm<Entity> entityRequsetForm) {
        try {
            validator(entityRequsetForm);
            List<Entity> entitys = validatorMasterDataList(entityRequsetForm, true);
            service.saveBatch(entitys);
            return ResultBody.success(entitys, "保存对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }


    /**
     * 模拟数据保存
     *
     * @return
     */
    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    @Override
    public ResultBody<Entity> mock() {
        try {
            MockConfig mockConfig = MockConfig.newInstance().setEnabledCircle(true).excludes("id").globalConfig();
            Entity entity = JMockData.mock(service.currentEntityClass(), mockConfig);
            entity = service.saveEntity(entity);
            return ResultBody.success(entity, "保存对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    /**
     * 删除
     *
     * @param entityRequsetForm
     * @return
     */
    @ApiOperation(value = "删除实体", notes = "删除实体")
    @PostMapping("/delete")
    @Override
    public ResultBody<Boolean> remove(@RequestBody(required = false) EntityRequsetForm<Entity> entityRequsetForm) {
        try {
            validator(entityRequsetForm);
            Entity entity = validatorMasterData(entityRequsetForm, true);
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
    @Override
    public ResultBody<Boolean> deleteByIds(@RequestBody(required = false) ObjectIdsForm objectIdsForm) {
        try {
            // 获取前端信息List<BusCompanyInfo>
            List<String> ids = objectIdsForm.getIds();
            if (CollectionUtil.isEmpty(ids)) {
                return ResultBody.error(true, "ID为空");
            }
            if (this.service.removeByIds(ids)) {
                return ResultBody.success(false, "批量成功刪除");
            }
            return ResultBody.error(false, "批量成功刪除");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }
}
