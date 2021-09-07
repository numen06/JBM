package com.jbm.cluster.bigscreen.controller;

import com.jbm.cluster.api.model.entity.bigscreen.BigscreenView;
import com.jbm.cluster.bigscreen.service.BigscreenViewService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-09-03 17:08:07
 */
@Api(tags = "大屏管理接口")
@RestController
@RequestMapping("/bigscreenView")
public class BigscreenViewController extends MasterDataCollection<BigscreenView, BigscreenViewService> {


    @ApiOperation(value = "上载大屏包", notes = "上载大屏包")
    @PostMapping("/upload")
    public ResultBody<BigscreenView> upload(@RequestBody(required = false) BigscreenView bigscreenView) {
        try {
            bigscreenView = service.upload(bigscreenView);
            return ResultBody.success(bigscreenView, "上载大屏包成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }


    @ApiOperation(value = "是否已经上传", notes = "是否已经上传")
    @PostMapping("/isUpload")
    public ResultBody<Boolean> isUpload(@RequestBody(required = false) BigscreenView bigscreenView) {
        try {
            Boolean wok = service.isUpload(bigscreenView);
            return ResultBody.success(wok, "判断成功");
        } catch (ServiceException e) {
            return ResultBody.error(null, e.getMessage(), e);
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }
}
