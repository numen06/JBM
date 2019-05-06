package com.jbm.framework.mvc.web;

import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.controller.IMasterDataTreeController;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.bean.MasterDataTreeEntity;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.usage.form.JsonRequestBody;
import com.jbm.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

public abstract class MasterDataTreeCollection<Entity extends MasterDataTreeEntity, TreeService extends IMasterDataTreeService<Entity>>
        extends MasterDataCollection implements IMasterDataTreeController<Entity, TreeService> {
    @Autowired
    protected TreeService treeService;

    public MasterDataTreeCollection() {
        super();
    }

    /**
     * 列表查询
     *
     * @param jsonRequestBody
     * @return
     */
    @RequestMapping("/root")
    @Override
    public Object root(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            if (ObjectUtils.isNull(jsonRequestBody)) {
                throw new ServiceException("参数错误");
            }
            Entity entity = jsonRequestBody.tryGet(treeService.getEntityClass());
            List<Entity> list = treeService.selectRootListById(entity);
            return ResultForm.success(list, "查询分页列表成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询分页列表失败", e);
        }
    }

    @RequestMapping("/rootByCode")
    @Override
    public Object rootByCode(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            if (ObjectUtils.isNull(jsonRequestBody)) {
                throw new ServiceException("参数错误");
            }
            Entity entity = jsonRequestBody.tryGet(treeService.getEntityClass());
            List<Entity> list = treeService.selectRootListByCode(entity);
            return ResultForm.success(list, "查询分页列表成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询分页列表失败", e);
        }
    }



    @RequestMapping("/tree")
    @Override
    public Object tree(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            if (ObjectUtils.isNull(jsonRequestBody)) {
                throw new ServiceException("参数错误");
            }
            Entity entity = jsonRequestBody.tryGet(treeService.getEntityClass());
            List<Entity> list = treeService.selectTreeByParentId(entity);
            return ResultForm.success(list, "查询分页列表成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询分页列表失败", e);
        }
    }

    @RequestMapping("/treeByCode")
    @Override
    public Object treeByCode(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            if (ObjectUtils.isNull(jsonRequestBody)) {
                throw new ServiceException("参数错误");
            }
            Entity entity = jsonRequestBody.tryGet(treeService.getEntityClass());
            List<Entity> list = treeService.selectTreeByParentCode(entity);
            return ResultForm.success(list, "查询分页列表成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询分页列表失败", e);
        }
    }


}
