package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseDeveloper;
import com.jbm.cluster.api.form.BaseDeveloperForm;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.common.mysql.service.BaseDeveloperService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统开发者管理
 */
@Api(tags = "系统开发者管理")
@RestController
@RequestMapping("/developer")
public class BaseDeveloperController {

    @Autowired
    private BaseDeveloperService baseDeveloperService;

    @ApiOperation(value = "开发者登录")
    @PostMapping("/sessions")
    public ResultBody<UserAccount> createSession(@RequestParam String username) {
        return ResultBody.callback(() -> baseDeveloperService.login(username));
    }

    @ApiOperation(value = "开发者分页列表")
    @GetMapping
    public ResultBody<DataPaging<BaseDeveloper>> listDevelopers(@ModelAttribute BaseDeveloperForm form) {
        return ResultBody.callback(() -> baseDeveloperService.findListPage(
                form != null ? form : new BaseDeveloperForm()));
    }

    @ApiOperation(value = "全部开发者")
    @GetMapping("/all")
    public ResultBody<List<BaseDeveloper>> listAllDevelopers() {
        return ResultBody.callback(() -> baseDeveloperService.findAllList());
    }

    @ApiOperation(value = "开发者详情")
    @GetMapping("/{userId}")
    public ResultBody<BaseDeveloper> getDeveloper(@PathVariable Long userId) {
        return ResultBody.callback(() -> baseDeveloperService.selectById(userId));
    }

    @ApiOperation(value = "创建开发者")
    @PostMapping
    public ResultBody<Void> createDeveloper(@RequestBody BaseDeveloperForm form) {
        baseDeveloperService.addUser(form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "更新开发者")
    @PutMapping("/{userId}")
    public ResultBody<Void> updateDeveloper(@PathVariable Long userId, @RequestBody BaseDeveloperForm form) {
        form.setUserId(userId);
        baseDeveloperService.updateUser(form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "更新密码")
    @PutMapping("/{userId}/password")
    public ResultBody<Void> updatePassword(@PathVariable Long userId, @RequestBody BaseDeveloperForm form) {
        baseDeveloperService.updatePassword(userId, form.getPassword());
        return ResultBody.ok();
    }

    @ApiOperation(value = "第三方开发者账号")
    @PostMapping("/third-party-accounts")
    public ResultBody<Void> createThirdPartyAccount(
            @RequestParam String account,
            @RequestParam String password,
            @RequestParam String accountType,
            @RequestParam String nickName,
            @RequestParam String avatar) {
        BaseDeveloper developer = new BaseDeveloper();
        developer.setNickName(nickName);
        developer.setUserName(account);
        developer.setPassword(password);
        developer.setAvatar(avatar);
        baseDeveloperService.addUserThirdParty(developer, accountType);
        return ResultBody.ok();
    }
}
