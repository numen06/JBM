package com.jbm.framework.mvc.web;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.jsonzou.jmockdata.JMockData;
import com.github.jsonzou.jmockdata.MockConfig;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.form.IdsForm;
import com.jbm.framework.masterdata.controller.IMasterDataController;
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
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * <strong>已废弃，计划删除。</strong>禁止在<strong>新建</strong> Controller 中继承本类；新代码必须使用<strong>显式 DTO</strong>与<strong>独立 REST 端点</strong>，不得从通用 {@code BaseRequsetBody} 反序列化实体。
 * <p>存量继承类请逐步迁移。本类中的 {@link #validatorMasterData} / {@link #validatorMasterDataList} 等从请求体直接还原实体的做法一并废弃。</p>
 *
 * @deprecated 自 7.3.0 起废弃，将在后续版本移除；请改用 DTO + 显式 Controller。
 */
@Deprecated
@Slf4j
public abstract class MasterDataCollection<Entity extends MasterDataEntity, Service extends IMasterDataService<Entity>>
        extends BaseController implements IMasterDataController<Entity> {
    @Autowired
    protected Service service;
    @Autowired
    protected MessageSource messageSource;

    public MasterDataCollection() {
        super();
    }


    protected void validator(BaseRequsetBody baseRequsetBody) throws RuntimeException {
        if (ObjectUtil.isNull(baseRequsetBody)) {
            throw new ServiceException("参数错误");
        }

    }

    /**
     * @deprecated 禁止继续使用：请使用 DTO 在 Controller 层接收参数，勿从 {@link BaseRequsetBody} 还原实体。
     */
    @Deprecated
    protected Entity validatorMasterData(BaseRequsetBody baseRequsetBody, Boolean valNull) throws RuntimeException {
        Entity entity = baseRequsetBody.tryGet(service.currentEntityClass());
        if (valNull) {
            if (ObjectUtil.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
        }
        return entity;
    }

    /**
     * @deprecated 禁止继续使用：请使用 DTO 列表在 Controller 层接收参数。
     */
    @Deprecated
    protected List<Entity> validatorMasterDataList(BaseRequsetBody baseRequsetBody, Boolean valNull) throws RuntimeException {
        List<Entity> entitys = baseRequsetBody.tryGetList(service.currentEntityClass());
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
     * @param pageRequestBody
     * @return
     */
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    @Override
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

    /**
     * 列表查询
     *
     * @param masterDataRequsetBody
     * @return
     */
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    @Override
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

    /**
     * 查询实体
     *
     * @param masterDataRequsetBody
     * @return
     */
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
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

    /**
     * 保存
     *
     * @param masterDataRequsetBody
     * @return
     */
    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    @Override
    public ResultBody<Entity> save(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            Entity entity = validatorMasterData(masterDataRequsetBody, true);
            entity = service.saveEntity(entity);
            return ResultBody.success(entity, "保存对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    /**
     * 保存多个对象
     *
     * @param masterDataRequsetBody
     * @return
     */
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<Entity>> saveBatch(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            List<Entity> entitys = validatorMasterDataList(masterDataRequsetBody, true);
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
     * @param pageRequestBody
     * @return
     */
    @ApiOperation(value = "删除实体", notes = "删除实体")
    @PostMapping("/delete")
    @Override
    public ResultBody<Boolean> remove(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            Entity entity = validatorMasterData(masterDataRequsetBody, true);
            if (service.deleteEntity(entity))
                return ResultBody.success(true, "删除对象成功");
            else
                return ResultBody.success(false, "删除对象失败");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "通过id删除实体", notes = "通过id删除实体")
    @PostMapping("/deleteByIds")
    @Override
    public ResultBody<Boolean> deleteByIds(@RequestBody(required = false) IdsForm idsForm) {
        try {
            // 获取前端信息List<BusCompanyInfo>
            List<Long> ids = idsForm.getIds();
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
