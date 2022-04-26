package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.entitys.message.PushMessage;
import com.jbm.cluster.common.security.utils.SecurityUtils;
import com.jbm.cluster.push.form.PushMessageForm;
import com.jbm.cluster.push.service.PushMessageService;
import com.jbm.framework.form.IdsForm;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-03-05 03:05:23
 */
@Api(tags = "站内信接口")
@RestController
@RequestMapping("/pushMessage")
public class PushMessageController extends MasterDataCollection<PushMessage, PushMessageService> {



    @ApiOperation("阅读")
    @PostMapping("/read")
    public ResultBody<String> read(@RequestBody IdsForm idsForm) {
        this.service.read(idsForm.getIds());
        return ResultBody.ok();
    }

    @ApiOperation("未阅读")
    @PostMapping("/unread")
    public ResultBody<String> unread(@RequestBody IdsForm idsForm) {
        this.service.unread(idsForm.getIds());
        return ResultBody.ok();
    }

    @ApiOperation("发送用户站内信")
    @PostMapping("/sendUserMessage")
    public ResultBody<String> sendUserMessage(@RequestBody PushMessage pushMessage) {
        this.service.sendUserMessage(pushMessage);
//        testLogFegin.findLogs(new GatewayLogsTest());
        return ResultBody.ok();
    }

    @ApiOperation("发送系统站内信")
    @PostMapping("/sendSysMessage")
    public ResultBody<String> sendSysMessage(@RequestBody PushMessage pushMessage) {

        this.service.sendSysMessage(pushMessage);
//        testLogFegin.findLogs(new GatewayLogsTest());
        return ResultBody.ok();
    }


    @ApiOperation("获取登录人的消息列表")
    @PostMapping("/findCurrMessagePage")
    public ResultBody<DataPaging<PushMessage>> findCurrMessagePage(@RequestBody PushMessageForm pushMessageform) {
        JbmLoginUser openUserDetails = SecurityUtils.getLoginUser();
        pushMessageform.getPushMessage().setRecUserId(openUserDetails.getUserid());
        pushMessageform.getPageForm().setSortRule("createTime:desc");
        DataPaging<PushMessage> dataPaging = this.service.selectEntitys(pushMessageform.getPushMessage(), pushMessageform.getPageForm());
        return ResultBody.success(dataPaging, "获取登录人的消息列表成功");
    }


}
