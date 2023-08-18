package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.job.SchedulerJob;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.push.PushMessageResult;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.push.form.PushMessageForm;
import com.jbm.cluster.push.message.PushMessageTest;
import com.jbm.cluster.push.service.PushMessageBodyService;
import com.jbm.cluster.push.service.PushMessageItemService;
import com.jbm.framework.form.IdsForm;
import com.jbm.framework.form.ObjectIdsForm;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-21 16:47:16
 */
@Api(tags = "消息推送项开放接口")
@RestController
@RequestMapping("/pushMessage")
public class PushMessageController {

    @Autowired
    private PushMessageBodyService pushMessageBodyService;

    @Autowired
    private PushMessageItemService pushMessageItemService;

    @ApiOperation("阅读")
    @PostMapping("/read")
    public ResultBody<String> read(@RequestBody ObjectIdsForm idsForm) {
        this.pushMessageItemService.read(idsForm.getIds());
        return ResultBody.success("标记已读成功");
    }

    @ApiOperation("未阅读")
    @PostMapping("/unread")
    public ResultBody<String> unread(@RequestBody ObjectIdsForm idsForm) {
        this.pushMessageItemService.unread(idsForm.getIds());
        return ResultBody.success("标记未F读成功");
    }

    @ApiOperation("查询消息列表")
    @PostMapping("/pageList")
    public ResultBody<DataPaging<PushMessageResult>> pageList(@RequestBody PushMessageForm pushMessageform) {
        DataPaging<PushMessageResult> dataPaging = this.pushMessageBodyService.selectPushMessagePageList(pushMessageform);
        return ResultBody.success(dataPaging, "查询消息列表成功");
    }


    @ApiOperation("获取登录人的消息列表")
    @PostMapping("/findCurrMessagePage")
    public ResultBody<DataPaging<PushMessageResult>> findCurrMessagePage(@RequestBody PushMessageForm pushMessageform) {
        JbmLoginUser jbmLoginUser = SecurityUtils.getLoginUser();
        pushMessageform.setRecUserId(jbmLoginUser.getUserId());
        pushMessageform.getPageForm().setSortRule("createTime:desc");
        DataPaging<PushMessageResult> dataPaging = this.pushMessageBodyService.findUserPushMessage(pushMessageform);
        return ResultBody.success(dataPaging, "获取登录人的消息列表成功");
    }

    @ApiOperation("发送用户站内信")
    @PostMapping("/sendUserMessage")
    public ResultBody<String> sendUserMessage(@RequestBody PushMessageBody pushMessageBody) {
        this.pushMessageBodyService.sendUserMessage(pushMessageBody);
        return ResultBody.ok();
    }

    @ApiOperation("发送系统站内信")
    @PostMapping("/sendSysMessage")
    public ResultBody<String> sendSysMessage(@RequestBody PushMessageBody pushMessageBody) {
        this.pushMessageBodyService.sendSysMessage(pushMessageBody);
        return ResultBody.ok();
    }

    @ApiOperation("删除站内信")
    @PostMapping("/deleteByIds")
    public ResultBody<String> deleteByIds(@RequestBody IdsForm idsForm) {
        this.pushMessageItemService.removeByIds(idsForm.getIds());
        return ResultBody.ok();
    }


    @Autowired
    private JbmClusterNotification jbmClusterNotification;

    @ApiOperation("发送推送消息")
    @PostMapping("/sendPushMsg")
    public ResultBody<String> sendPushMsg(@RequestBody PushMsg pushMsg) {
        jbmClusterNotification.pushMsg(pushMsg);
        return ResultBody.ok("发送推送消息成功");
    }


    @Autowired
    private PushMessageTest pushMessageTest;

    @SchedulerJob(cron = "0/5 * *  * * ? ", name = "发送管理员测试信息", enable = false)
    @ApiOperation("发送管理员测试信息")
    @PostMapping("/testSend")
    public ResultBody<String> testSend() {
        pushMessageTest.testSend();
        return ResultBody.ok();
    }

}
