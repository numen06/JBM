package com.jbm.framework.mvc.web;

import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.controller.IMasterDataController;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.framework.usage.form.JsonRequestBody;
import com.jbm.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

public abstract class MasterDataCollection<Entity extends MasterDataEntity, Service extends IMasterDataService<Entity>>
        extends BaseCollection implements IMasterDataController<Entity, Service> {
    @Autowired
    protected Service service;
    @Autowired
    protected MessageSource messageSource;

    public MasterDataCollection() {
        super();
    }

    /**
     * 列表查询
     *
     * @param jsonRequestBody
     * @return
     */
    @RequestMapping("/pageList")
    @Override
    public Object pageList(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            if (ObjectUtils.isNull(jsonRequestBody)) {
                throw new ServiceException("参数错误");
            }
            Entity entity = jsonRequestBody.tryGet(service.getEntityClass());
            PageForm pageForm = jsonRequestBody.getPageForm();
            DataPaging<Entity> dataPaging = service.selectEntitys(entity, pageForm);
            return ResultForm.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询分页列表失败", e);
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
            if (ObjectUtils.isNull(jsonRequestBody)) {
                throw new ServiceException("参数错误");
            }
            Entity entity = jsonRequestBody.tryGet(service.getEntityClass());
            if (ObjectUtils.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
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
            if (ObjectUtils.isNull(jsonRequestBody)) {
                throw new ServiceException("参数错误");
            }
            Entity entity = jsonRequestBody.tryGet(service.getEntityClass());
            if (ObjectUtils.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
            entity = service.saveEntity(entity);
            return ResultForm.success(entity, "保存对象成功");
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
            if (ObjectUtils.isNull(jsonRequestBody)) {
                throw new ServiceException("参数错误");
            }
            final Entity entity = jsonRequestBody.tryGet(service.getEntityClass());
            if (ObjectUtils.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
            service.delete(entity);
            // smsConfService.delete(smsConf);
            return ResultForm.success(entity, "删除对象成功");
        } catch (Exception e) {
            System.err.println(e);
            return ResultForm.error(null, "删除对象失败");
        }
    }
}
