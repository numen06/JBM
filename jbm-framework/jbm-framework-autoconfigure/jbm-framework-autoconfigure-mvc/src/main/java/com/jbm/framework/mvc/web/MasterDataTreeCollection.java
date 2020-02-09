package com.jbm.framework.mvc.web;

import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.controller.IMasterDataTreeController;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.bean.MasterDataTreeEntity;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.usage.form.JsonRequestBody;
import com.jbm.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

public abstract class MasterDataTreeCollection<Entity extends MasterDataTreeEntity, Service extends IMasterDataTreeService<Entity>>
        extends MasterDataCollection<Entity, Service> implements IMasterDataTreeController<Entity, Service> {

    public MasterDataTreeCollection() {
        super();
    }

    @Override
    protected Entity validatorMasterData(JsonRequestBody jsonRequestBody, Boolean valNull) throws Exception {
        Entity entity = jsonRequestBody.tryGet(service.getEntityClass());
        if (valNull) {
            if (ObjectUtils.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
        }
        return entity;
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
            validator(jsonRequestBody);
            Entity entity = validatorMasterData(jsonRequestBody, false);
            List<Entity> list = service.selectRootListById(entity);
            return ResultForm.success(list, "查询树根节点列表成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询树根节点列表成功", e);
        }
    }

    @RequestMapping("/rootByCode")
    @Override
    public Object rootByCode(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            validator(jsonRequestBody);
            Entity entity = validatorMasterData(jsonRequestBody, false);
            List<Entity> list = service.selectRootListByCode(entity);
            return ResultForm.success(list, "通过Code查询树根节点列表成功");
        } catch (Exception e) {
            return ResultForm.error(null, "通过Code查询树根节点列表成功", e);
        }
    }


    @RequestMapping("/tree")
    @Override
    public Object tree(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            validator(jsonRequestBody);
            Entity entity = validatorMasterData(jsonRequestBody, false);
            List<Entity> list = service.selectChildNodesById(entity);
            return ResultForm.success(list, "查询树结构成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询树结构成功", e);
        }
    }

    @RequestMapping("/treeByCode")
    @Override
    public Object treeByCode(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
        try {
            validator(jsonRequestBody);
            Entity entity = validatorMasterData(jsonRequestBody, false);
            List<Entity> list = service.selectChildNodesByCode(entity);
            return ResultForm.success(list, "通过Code查询树结构成功");
        } catch (Exception e) {
            return ResultForm.error(null, "通过Code查询树结构成功", e);
        }
    }


}
