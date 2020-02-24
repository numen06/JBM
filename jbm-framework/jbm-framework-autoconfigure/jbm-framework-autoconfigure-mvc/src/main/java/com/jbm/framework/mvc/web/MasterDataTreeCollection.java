package com.jbm.framework.mvc.web;

import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.controller.IMasterDataTreeController;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.usage.form.BaseRequsetBody;
import com.jbm.util.ObjectUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Slf4j
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
     * @param baseRequsetBody
     * @return
     */
    @ApiOperation(value = "获取根节点列表", notes = "获取根节点列表")
    @PostMapping("/root")
    @Override
    public ResultForm root(@RequestBody(required = false) BaseRequsetBody baseRequsetBody) {
        try {
            validator(baseRequsetBody);
            List<Entity> list = service.selectRootListById();
            return ResultForm.success(list, "查询树根节点列表成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询树根节点列表失败", e);
        }
    }

    @ApiOperation(value = "获取根树状结构", notes = "获取根树状结构")
    @PostMapping("/tree")
    @Override
    public ResultForm tree(@RequestBody(required = false) BaseRequsetBody baseRequsetBody) {
        try {
            validator(baseRequsetBody);
            Entity entity = validatorMasterData(baseRequsetBody, false);
            List<Entity> list = service.selectChildNodesById(entity);
            return ResultForm.success(list, "查询树结构成功");
        } catch (Exception e) {
            return ResultForm.error(null, "查询树结构失败", e);
        }
    }


}
