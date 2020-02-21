package com.jbm.framework.mvc.web;

import cn.hutool.core.collection.CollectionUtil;
import com.github.jsonzou.jmockdata.JMockData;
import com.github.jsonzou.jmockdata.MockConfig;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.controller.IMasterDataController;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.usage.form.BaseRequsetBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
public abstract class MasterDataCollection<Entity extends MasterDataEntity, Service extends IMasterDataService<Entity>>
        extends BaseCollection implements IMasterDataController<Entity, Service> {
    @Autowired
    protected Service service;
    @Autowired
    protected MessageSource messageSource;

    public MasterDataCollection() {
        super();
    }


    protected void validator(BaseRequsetBody baseRequsetBody) throws Exception {
        if (ObjectUtils.isNull(baseRequsetBody)) {
            throw new ServiceException("参数错误");
        }

    }

    protected Entity validatorMasterData(BaseRequsetBody baseRequsetBody, Boolean valNull) throws Exception {
        Entity entity = baseRequsetBody.tryGet(service.currentEntityClass());
        if (valNull) {
            if (ObjectUtils.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
        }
        return entity;
    }

    protected List<Entity> validatorMasterDataList(BaseRequsetBody baseRequsetBody, Boolean valNull) throws Exception {
        List<Entity> entitys = baseRequsetBody.tryGetList(service.currentEntityClass());
        if (valNull) {
            if (CollectionUtil.isEmpty(entitys)) {
                throw new ServiceException("列表参数为空");
            }
            if (ObjectUtils.isNull(entitys)) {
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
    @RequestMapping("/pageList")
    @Override
    public Object pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            Entity entity = validatorMasterData(pageRequestBody, false);
            PageForm pageForm = pageRequestBody.getPageForm();
            DataPaging<Entity> dataPaging = service.selectPageList(entity, pageForm);
            return ResultForm.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultForm.error(e);
        }
    }

    /**
     * 列表查询
     *
     * @param pageRequestBody
     * @return
     */
    @RequestMapping("/list")
    @Override
    public Object list(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            Entity entity = validatorMasterData(pageRequestBody, false);
            List<Entity> list = service.selectPageList(entity);
            return ResultForm.success(list, "查询列表成功");
        } catch (Exception e) {
            return ResultForm.error(e);
        }
    }

    /**
     * 查询实体
     *
     * @param pageRequestBody
     * @return
     */
    @RequestMapping("/model")
    @Override
    public Object model(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            Entity entity = validatorMasterData(pageRequestBody, true);
            entity = service.selectEntity(entity);
            return ResultForm.success(entity, "查询对象成功");
        } catch (Exception e) {
            return ResultForm.error(e);
        }
    }

    /**
     * 保存
     *
     * @param pageRequestBody
     * @return
     */
    @RequestMapping("/save")
    @Override
    public Object save(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            Entity entity = validatorMasterData(pageRequestBody, true);
            entity = service.saveEntity(entity);
            return ResultForm.success(entity, "保存对象成功");
        } catch (Exception e) {
            return ResultForm.error(e);
        }
    }

    /**
     * 保存多个对象
     *
     * @param pageRequestBody
     * @return
     */
    @RequestMapping("/saveBatch")
    @Override
    public Object saveBatch(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            List<Entity> entitys = validatorMasterDataList(pageRequestBody, true);
            service.saveBatch(entitys);
            return ResultForm.success(entitys, "保存对象成功");
        } catch (Exception e) {
            return ResultForm.error(e);
        }
    }


    /**
     * 模拟数据保存
     *
     * @return
     */
    @RequestMapping("/mock")
    @Override
    public Object mock() {
        try {
            MockConfig mockConfig = MockConfig.newInstance().setEnabledCircle(true).excludes("id").globalConfig();
            Entity entity = JMockData.mock(service.currentEntityClass(), mockConfig);
            entity = service.saveEntity(entity);
            return ResultForm.success(entity, "保存对象成功");
        } catch (Exception e) {
            return ResultForm.error(e);
        }
    }

    /**
     * 删除
     *
     * @param pageRequestBody
     * @return
     */
    @RequestMapping("/delete")
    @Override
    public Object remove(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            Entity entity = validatorMasterData(pageRequestBody, true);
            if (service.deleteEntity(entity))
                return ResultForm.success(entity, "删除对象成功");
            else
                return ResultForm.success(entity, "删除对象失败");
        } catch (Exception e) {
            return ResultForm.error(e);
        }
    }

    @RequestMapping("/deleteByIds")
    @Override
    public Object deleteByIds(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            // 获取前端信息List<BusCompanyInfo>
            validator(pageRequestBody);
            List<Long> ids = pageRequestBody.getList("ids", Long.class);
            if (CollectionUtil.isEmpty(ids)) {
                throw new ServiceException("ID为空");
            }
            this.service.removeByIds(ids);
            return ResultForm.success("success", "批量成功刪除");
        } catch (Exception e) {
            return ResultForm.error(e);
        }
    }
}
