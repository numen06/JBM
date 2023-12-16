package com.jbm.framework.mvc.web;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.controller.IMasterDataTreeController;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.form.BaseRequsetBody;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Slf4j
public abstract class MasterDataTreeCollection<Entity extends MasterDataTreeEntity, Service extends IMasterDataTreeService<Entity>>
        extends MasterDataCollection<Entity, Service> implements IMasterDataTreeController<Entity> {

    public MasterDataTreeCollection() {
        super();
    }

    @Override
    protected Entity validatorMasterData(BaseRequsetBody pageRequestBody, Boolean valNull) throws Exception {
        Entity entity = pageRequestBody.tryGet(service.currentEntityClass());
        if (valNull) {
            if (ObjectUtil.isNull(entity)) {
                throw new ServiceException("参数错误");
            }
        }
        return entity;
    }


    /**
     * 列表查询
     *
     * @param masterDataRequsetBody
     * @return
     */
    @ApiOperation(value = "获取根节点列表", notes = "获取根节点列表")
    @PostMapping("/root")
    @Override
    public ResultBody<List<Entity>> root(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            List<Entity> list = service.selectRootListById();
            return ResultBody.success(list, "查询树根节点列表成功");
        } catch (Exception e) {
            return ResultBody.error(null, "查询树根节点列表失败", e);
        }
    }

    @ApiOperation(value = "获取根树状结构", notes = "获取根树状结构")
    @PostMapping("/tree")
    @Override
    public ResultBody<List<Entity>> tree(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            Entity entity = validatorMasterData(masterDataRequsetBody, false);
            List<Entity> list = service.selectChildNodesById(entity);
            return ResultBody.success(list, "查询树结构成功");
        } catch (Exception e) {
            return ResultBody.error(null, "查询树结构失败", e);
        }
    }


}
