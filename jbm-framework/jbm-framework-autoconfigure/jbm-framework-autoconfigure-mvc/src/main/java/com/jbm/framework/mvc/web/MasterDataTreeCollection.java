package com.jbm.framework.mvc.web;

import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.controller.IMasterDataTreeController;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.usage.form.BaseRequsetBody;
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
    protected Entity validatorMasterData(BaseRequsetBody pageRequestBody, Boolean valNull) throws Exception {
        Entity entity = pageRequestBody.tryGet(service.currentEntityClass());
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
     * @param pageRequestBody
     * @return
     */
    @RequestMapping("/root")
    @Override
    public Object root(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            List<Entity> list = service.selectRootListById();
            return ResultForm.success(list, "查询树根节点列表成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询树根节点列表成功", e);
        }
    }


    @RequestMapping("/tree")
    @Override
    public Object tree(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            Entity entity = validatorMasterData(pageRequestBody, false);
            List<Entity> list = service.selectTreeByParentId(entity);
            return ResultForm.success(list, "查询树结构成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询树结构成功", e);
        }
    }


}
