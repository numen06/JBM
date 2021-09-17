package com.jbm.framework.mvc.web;

import com.jbm.framework.masterdata.controller.IMultiPlatformTreeController;
import com.jbm.framework.masterdata.service.IMultiPlatformTreeService;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformTreeEntity;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Slf4j
public abstract class MultiPlatformTreeCollection<Entity extends MultiPlatformTreeEntity, Service extends IMultiPlatformTreeService<Entity>>
        extends MasterDataCollection<Entity, Service> implements IMultiPlatformTreeController<Entity> {

    public MultiPlatformTreeCollection() {
        super();
    }

    /**
     * 列表查询
     *
     * @return
     */
    @ApiOperation(value = "获取根节点列表", notes = "获取根节点列表")
    @PostMapping("/root")
    @Override
    public ResultBody<List<Entity>> root(@RequestBody(required = false) Entity entity) {
        try {
            List<Entity> list = service.selectRootListById(entity);
            return ResultBody.success(list, "查询树根节点列表成功");
        } catch (Exception e) {
            return ResultBody.error(null, "查询树根节点列表失败", e);
        }
    }

    @ApiOperation(value = "获取根树状结构", notes = "获取根树状结构")
    @PostMapping("/tree")
    @Override
    public ResultBody<List<Entity>> tree(@RequestBody(required = false) Entity entity) {
        try {
            List<Entity> list = service.selectChildNodesById(entity);
            return ResultBody.success(list, "查询树结构成功");
        } catch (Exception e) {
            return ResultBody.error(null, "查询树结构失败", e);
        }
    }


}
