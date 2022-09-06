package com.jbm.cluster.push.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.push.PushStatus;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushCallback;
import com.jbm.cluster.push.form.PushMessageForm;
import com.jbm.cluster.push.service.PushMessageItemService;
import com.jbm.cluster.push.usage.PushMessageNotificationExchanger;
import com.jbm.framework.masterdata.utils.EntityUtils;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-21 16:47:16
 */
@Service
public class PushMessageItemServiceImpl extends MasterDataServiceImpl<PushMessageItem> implements PushMessageItemService {


    @Autowired
    private PushMessageNotificationExchanger pushMessageNotificationExchanger;

    @Override
    public boolean read(List<String> ids) {
//        ids.forEach(id -> read(id));
        Assert.noNullElements(ids, "推送ID不能为空");
        UpdateWrapper<PushMessageItem> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(PushMessageItem::getReadFlag, true).in(PushMessageItem::getMsgId, ids);
        return this.update(updateWrapper);
//        List<PushMessageItem> list = new ArrayList<>();
//        ids.forEach(new Consumer<String>() {
//            @Override
//            public void accept(String id) {
//                PushMessageItem pushMessageItem = new PushMessageItem();
//                pushMessageItem.setReadFlag(true);
//                pushMessageItem.setMsgId(id);
//                list.add(pushMessageItem);
//            }
//        });
//        return this.updateBatchById(list);
    }

    @Override
    public boolean unread(List<String> ids) {
        Assert.noNullElements(ids, "推送ID不能为空");
        UpdateWrapper<PushMessageItem> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(PushMessageItem::getReadFlag, false).in(PushMessageItem::getMsgId, ids);
        return this.update(updateWrapper);
    }

    @Override
    public boolean read(String id) {
        return this.read(Lists.newArrayList(id));
    }

    @Override
    public boolean unread(String id) {
        return this.unread(Lists.newArrayList(id));
    }

    @Override
    public String toPush(PushWay pushWay, PushMessageBody pushMessageBody, Long recUserId) {
        PushMessageItem pushMessageItem = new PushMessageItem();
        pushMessageItem.setPushStatus(PushStatus.unsent);
        pushMessageItem.setMsgBodyId(pushMessageBody.getId());
        pushMessageItem.setReadFlag(false);
        pushMessageItem.setPushWay(pushWay);
        pushMessageItem.setSendUserId(pushMessageBody.getSendUserId());
        pushMessageItem.setRecUserId(recUserId);
        pushMessageItem = saveEntity(pushMessageItem);
        pushMessageNotificationExchanger.exchange(pushMessageBody, pushMessageItem);
        return pushMessageItem.getMsgId();
    }

    /**
     * 通过回调方法修改发送状态
     *
     * @param pushCallback
     * @return
     */
    @Override
    public void sendCallBack(PushCallback pushCallback) {
        PushMessageItem pushMessageItem = new PushMessageItem();
        pushMessageItem.setMsgId(pushCallback.getMsgId());
        pushMessageItem.setPushStatus(pushCallback.getPushStatus());
        this.updateById(pushMessageItem);
    }

    @Override
    public DataPaging<PushMessageItem> findUserPushMessage(PushMessageForm pushMessageform) {
        QueryWrapper<PushMessageItem> queryWrapper = currentQueryWrapper();
        queryWrapper.eq(EntityUtils.toDbName(PushMessageItem::getRecUserId), pushMessageform.getRecUserId());
        return this.selectEntitysByWapper(queryWrapper, pushMessageform.getPageForm());
    }
}