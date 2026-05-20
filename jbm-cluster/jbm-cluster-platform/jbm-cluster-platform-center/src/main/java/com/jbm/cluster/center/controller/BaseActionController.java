package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseAction;
import com.jbm.cluster.api.form.BaseActionForm;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.mysql.service.BaseActionService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统功能按钮管理
 */
@Api(tags = "系统功能按钮管理")
@RestController
@RequestMapping("/action")
public class BaseActionController extends BaseController {

    @Autowired
    private BaseActionService baseActionService;
    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    @ApiOperation(value = "操作分页列表")
    @GetMapping
    public ResultBody<DataPaging<BaseAction>> listActions(@ModelAttribute BaseActionForm form) {
        return ResultBody.callback(() -> baseActionService.findListPage(form != null ? form : new BaseActionForm()));
    }

    @ApiOperation(value = "操作详情")
    @GetMapping("/{actionId}")
    public ResultBody<BaseAction> getAction(@PathVariable Long actionId) {
        return ResultBody.callback(() -> baseActionService.getAction(actionId));
    }

    @ApiOperation(value = "创建操作")
    @PostMapping
    public ResultBody<Long> createAction(@RequestBody BaseActionForm form) {
        return ResultBody.callback(() -> {
            BaseAction result = baseActionService.addAction(form);
            if (result != null) {
                jbmClusterTemplate.refreshGateway();
                return result.getActionId();
            }
            return null;
        });
    }

    @ApiOperation(value = "更新操作")
    @PutMapping("/{actionId}")
    public ResultBody<Void> updateAction(@PathVariable Long actionId, @RequestBody BaseActionForm form) {
        form.setActionId(actionId);
        baseActionService.updateAction(form);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }

    @ApiOperation(value = "删除操作")
    @DeleteMapping("/{actionId}")
    public ResultBody<Void> deleteAction(@PathVariable Long actionId) {
        baseActionService.removeAction(actionId);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }
}
