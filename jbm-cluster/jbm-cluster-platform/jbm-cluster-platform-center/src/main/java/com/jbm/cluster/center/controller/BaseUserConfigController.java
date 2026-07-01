package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseUserConfig;
import com.jbm.cluster.center.service.BaseUserConfigService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2021-08-25 11:19:05
 */
@Api(tags = "用户配置管理")
@RestController
@RequestMapping("/baseUserConfig")
public class BaseUserConfigController extends MasterDataCollection<BaseUserConfig, BaseUserConfigService> {

    @SaCheckLogin
    @ApiOperation(value = "保存用户配置", notes = "当前用户保存时可不传 userId，服务端按 token 识别并 upsert；管理员可指定其他用户 userId")
    @PostMapping("/save")
    @Override
    public ResultBody<BaseUserConfig> save(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            BaseUserConfig entity = validatorMasterData(masterDataRequsetBody, true);
            entity = service.saveEntity(entity);
            return ResultBody.success(entity, "保存对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @SaCheckLogin
    @ApiOperation(value = "获取用户配置", notes = "当前用户查询时可不传 userId，服务端按 token 与 appId 查询")
    @PostMapping("/model")
    @Override
    public ResultBody<BaseUserConfig> model(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            BaseUserConfig entity = masterDataRequsetBody.tryGet(BaseUserConfig.class);
            Long currentUserId = LoginHelper.getUserId();
            if (entity == null
                    || ObjectUtil.isEmpty(entity.getUserId())
                    || ObjectUtil.equal(entity.getUserId(), currentUserId)) {
                return ResultBody.callback(() -> service.findByUserIdAndAppId(
                        currentUserId, LoginHelper.getLoginUser().getAppId()));
            }
            entity = validatorMasterData(masterDataRequsetBody, true);
            if (ObjectUtil.isNotEmpty(entity.getUserId()) && ObjectUtil.isNotEmpty(entity.getAppId())) {
                BaseUserConfig query = entity;
                return ResultBody.callback(() -> service.findByUserIdAndAppId(query.getUserId(), query.getAppId()));
            }
            entity = service.selectEntity(entity);
            return ResultBody.success(entity, "查询对象成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }
}
