package com.jbm.framework.mvc.web;

import cn.hutool.core.collection.CollectionUtil;
import com.github.jsonzou.jmockdata.JMockData;
import com.github.jsonzou.jmockdata.MockConfig;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.controller.IMasterDataController;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.usage.form.JsonRequestBody;
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


    protected void validator(JsonRequestBody jsonRequestBody) throws Exception {
        if (ObjectUtils.isNull(jsonRequestBody)) {
            throw new ServiceException("参数错误");
        }

    }

    protected Entity validatorMasterData(JsonRequestBody jsonRequestBody, Boolean valNull) throws Exception {
        Entity entity = jsonRequestBody.tryGet(service.getEntityClass());
        if (valNull) {
            if (ObjectUtils.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
        }
        return entity;
    }

    protected List<Entity> validatorMasterDataList(JsonRequestBody jsonRequestBody, Boolean valNull) throws Exception {
        List<Entity> entitys = jsonRequestBody.tryGetList(service.getEntityClass());
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
     * @param jsonRequestBody
     * @return
     */
    @RequestMapping("/pageList")
    @Override
    public Object pageList(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            validator(jsonRequestBody);
            Entity entity = validatorMasterData(jsonRequestBody, false);
            PageForm pageForm = jsonRequestBody.getPageForm();
            DataPaging<Entity> dataPaging = service.selectEntitys(entity, pageForm);
            return ResultForm.success(dataPaging, "查询分页列表成功");
        } catch (ServiceException e) {
            return ResultForm.error(null, e.getMessage(), e);
        } catch (Exception e) {
            return ResultForm.error(null, "查询分页列表失败", e);
        }
    }

    /**
     * 列表查询
     *
     * @param jsonRequestBody
     * @return
     */
    @RequestMapping("/list")
    @Override
    public Object list(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            validator(jsonRequestBody);
            Entity entity = validatorMasterData(jsonRequestBody, false);
            List<Entity> list = service.selectEntitys(entity);
            return ResultForm.success(list, "查询列表成功");
        } catch (ServiceException e) {
            return ResultForm.error(null, e.getMessage(), e);
        } catch (Exception e) {
            return ResultForm.error(null, "查询列表失败", e);
        }
    }

    /**
     * 查询实体
     *
     * @param jsonRequestBody
     * @return
     */
    @RequestMapping("/model")
    @Override
    public Object model(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            validator(jsonRequestBody);
            Entity entity = validatorMasterData(jsonRequestBody, true);
            entity = service.selectEntity(entity);
            return ResultForm.success(entity, "查询对象成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询对象失败");
        }
    }

    /**
     * 保存
     *
     * @param jsonRequestBody
     * @return
     */
    @RequestMapping("/save")
    @Override
    public Object save(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            validator(jsonRequestBody);
            Entity entity = validatorMasterData(jsonRequestBody, true);
            entity = service.saveEntity(entity);
            return ResultForm.success(entity, "保存对象成功");
        } catch (ServiceException e) {
            return ResultForm.error(null, e.getMessage(), e);
        } catch (Exception e) {
            return ResultForm.error(null, "保存对象失败");
        }
    }

    /**
     * 保存多个对象
     *
     * @param jsonRequestBody
     * @return
     */
    @RequestMapping("/saveBatch")
    @Override
    public Object saveBatch(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            validator(jsonRequestBody);
            List<Entity> entitys = validatorMasterDataList(jsonRequestBody, true);
            service.saveBatch(entitys);
            return ResultForm.success(entitys, "保存对象成功");
        } catch (ServiceException e) {
            return ResultForm.error(null, e.getMessage(), e);
        } catch (Exception e) {
            return ResultForm.error(null, "保存对象失败");
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
            Entity entity = JMockData.mock(service.getEntityClass(), mockConfig);
            entity = service.saveEntity(entity);
            return ResultForm.success(entity, "保存对象成功");
        } catch (ServiceException e) {
            return ResultForm.error(null, e.getMessage(), e);
        } catch (Exception e) {
            return ResultForm.error(null, "保存对象失败");
        }
    }

    /**
     * 删除
     *
     * @param jsonRequestBody
     * @return
     */
    @RequestMapping("/delete")
    @Override
    public Object remove(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            validator(jsonRequestBody);
            Entity entity = validatorMasterData(jsonRequestBody, true);
            service.delete(entity);
            // smsConfService.delete(smsConf);
            return ResultForm.success(entity, "删除对象成功");
        } catch (ServiceException e) {
            return ResultForm.error(null, e.getMessage(), e);
        } catch (Exception e) {
            System.err.println(e);
            return ResultForm.error(null, "删除对象失败");
        }
    }

    @RequestMapping("/deleteByIds")
    @Override
    public Object deleteByIds(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            // 获取前端信息List<BusCompanyInfo>
            validator(jsonRequestBody);
            List<Long> ids = jsonRequestBody.getList("ids", Long.class);
            if (CollectionUtil.isEmpty(ids)) {
                throw new ServiceException("ID为空");
            }
            this.service.removeByIds(ids);
            return ResultForm.success("success", "批量成功刪除");
        } catch (ServiceException e) {
            return ResultForm.error(null, e.getMessage(), e);
        } catch (Exception e) {
            return ResultForm.error(e, e.getMessage());
        }
    }
}
