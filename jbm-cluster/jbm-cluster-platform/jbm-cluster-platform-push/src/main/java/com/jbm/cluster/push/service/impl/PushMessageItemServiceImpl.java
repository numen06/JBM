package com.jbm.cluster.push.service.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.collect.Lists;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.push.PushStatus;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushCallback;
import com.jbm.cluster.push.form.PushMessageForm;
import com.jbm.cluster.push.service.PushRealtimeMessageService;
import com.jbm.cluster.push.service.PushMessageItemService;
import com.jbm.cluster.push.usage.PushMessageNotificationExchanger;
import com.jbm.framework.masterdata.utils.EntityUtils;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.batch.BatchTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Consumer;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-21 16:47:16
 */
@Service
public class PushMessageItemServiceImpl extends MasterDataServiceImpl<PushMessageItem> implements PushMessageItemService {

    private static final String EXCLUDE_TEST_MESSAGE_BODY_SQL = "select id from push_message_body where template_code = '__push_test__' or ((template_code is null or template_code <> '__push_test_visible__') and title in ('Push通讯测试', 'Push 通讯测试'))";

    @Autowired
    private PushMessageNotificationExchanger pushMessageNotificationExchanger;
    @Autowired
    private PushRealtimeMessageService pushRealtimeMessageService;

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
    public boolean readAllForUser(Long recUserId) {
        UpdateWrapper<PushMessageItem> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set(EntityUtils.toDbName(PushMessageItem::getReadFlag), true);
        updateWrapper.and(wrapper -> wrapper
                .eq(EntityUtils.toDbName(PushMessageItem::getRecUserId), recUserId)
                .or()
                .eq(EntityUtils.toDbName(PushMessageItem::getRecUserId), 0L));
        updateWrapper.eq(EntityUtils.toDbName(PushMessageItem::getReadFlag), false);
        excludeTestMessages(updateWrapper);
        return this.update(updateWrapper);
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
        if (PushWay.internal == pushWay) {
            pushRealtimeMessageService.publish(pushMessageBody, pushMessageItem);
        }
        pushMessageNotificationExchanger.exchange(pushMessageBody, pushMessageItem);
        return pushMessageItem.getMsgId();
    }


    private final BatchTask<PushMessageItem> batchTask = new BatchTask<>(new Consumer<List<PushMessageItem>>() {
        @Override
        public void accept(List<PushMessageItem> pushMessageItems) {
            //批量更新
            updateBatchById(pushMessageItems);
//            pushMessageItems.parallelStream().forEach(new Consumer<PushMessageItem>() {
//                @Override
//                public void accept(PushMessageItem pushMessageItem) {
//                    updateById(pushMessageItem);
//                }
//            });
        }
    });

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
        //批量保存
        batchTask.offer(pushMessageItem);
    }

    @Override
    public DataPaging<PushMessageItem> pageList(PushMessageForm pushMessageform) {
        QueryWrapper<PushMessageItem> queryWrapper = currentQueryWrapper();
        selectExistingColumns(queryWrapper);
        if (!Boolean.TRUE.equals(pushMessageform.getIncludeTestMessages())) {
            excludeTestMessages(queryWrapper);
        }
        if (pushMessageform.getRecUserId() != null) {
            queryWrapper.eq(EntityUtils.toDbName(PushMessageItem::getRecUserId), pushMessageform.getRecUserId());
        }
        if (pushMessageform.getReadFlag() != null) {
            queryWrapper.eq(EntityUtils.toDbName(PushMessageItem::getReadFlag), pushMessageform.getReadFlag());
        }
        applyItemFilters(queryWrapper, pushMessageform);
        applyBodyFilters(queryWrapper, pushMessageform);
        applyKeyword(queryWrapper, pushMessageform);
        return this.selectEntitysByWapper(queryWrapper, pushMessageform.getPageForm());
    }

    @Override
    public DataPaging<PushMessageItem> findUserPushMessage(PushMessageForm pushMessageform) {
        QueryWrapper<PushMessageItem> queryWrapper = currentQueryWrapper();
        selectExistingColumns(queryWrapper);
        queryWrapper.and(wrapper -> wrapper
                .eq(EntityUtils.toDbName(PushMessageItem::getRecUserId), pushMessageform.getRecUserId())
                .or()
                .eq(EntityUtils.toDbName(PushMessageItem::getRecUserId), 0L));
        excludeTestMessages(queryWrapper);
        if (pushMessageform.getReadFlag() != null) {
            queryWrapper.eq(EntityUtils.toDbName(PushMessageItem::getReadFlag), pushMessageform.getReadFlag());
        }
        applyItemFilters(queryWrapper, pushMessageform);
        applyBodyFilters(queryWrapper, pushMessageform);
        applyKeyword(queryWrapper, pushMessageform);
        return this.selectEntitysByWapper(queryWrapper, pushMessageform.getPageForm());
    }

    @Override
    public long countUnread(Long recUserId) {
        QueryWrapper<PushMessageItem> queryWrapper = currentQueryWrapper();
        queryWrapper.and(wrapper -> wrapper
                .eq(EntityUtils.toDbName(PushMessageItem::getRecUserId), recUserId)
                .or()
                .eq(EntityUtils.toDbName(PushMessageItem::getRecUserId), 0L));
        queryWrapper.eq(EntityUtils.toDbName(PushMessageItem::getReadFlag), false);
        excludeTestMessages(queryWrapper);
        return this.count(queryWrapper);
    }

    private void selectExistingColumns(QueryWrapper<PushMessageItem> queryWrapper) {
        queryWrapper.select("msg_id", "msg_body_id", "rec_user_id", "send_user_id", "push_status", "push_way", "read_flag", "create_time", "update_time");
    }

    private void excludeTestMessages(AbstractWrapper<PushMessageItem, String, ?> queryWrapper) {
        queryWrapper.notInSql(EntityUtils.toDbName(PushMessageItem::getMsgBodyId), EXCLUDE_TEST_MESSAGE_BODY_SQL);
    }

    private void applyItemFilters(QueryWrapper<PushMessageItem> queryWrapper, PushMessageForm pushMessageform) {
        if (pushMessageform == null) {
            return;
        }
        if (pushMessageform.getPushWay() != null) {
            queryWrapper.eq(EntityUtils.toDbName(PushMessageItem::getPushWay), pushMessageform.getPushWay());
        }
        if (pushMessageform.getPushStatus() != null) {
            queryWrapper.eq(EntityUtils.toDbName(PushMessageItem::getPushStatus), pushMessageform.getPushStatus());
        }
        String sourceType = StrUtil.trim(pushMessageform.getSourceType());
        if ("system".equalsIgnoreCase(sourceType)) {
            queryWrapper.isNull(EntityUtils.toDbName(PushMessageItem::getSendUserId));
        } else if ("user".equalsIgnoreCase(sourceType)) {
            queryWrapper.isNotNull(EntityUtils.toDbName(PushMessageItem::getSendUserId));
        }
    }

    private void applyBodyFilters(QueryWrapper<PushMessageItem> queryWrapper, PushMessageForm pushMessageform) {
        if (pushMessageform == null || pushMessageform.getType() == null) {
            return;
        }
        queryWrapper.apply(
                EntityUtils.toDbName(PushMessageItem::getMsgBodyId)
                        + " in (select id from push_message_body where type = {0})",
                pushMessageform.getType().name());
    }

    private void applyKeyword(QueryWrapper<PushMessageItem> queryWrapper, PushMessageForm pushMessageform) {
        if (pushMessageform == null || pushMessageform.getPageForm() == null) {
            return;
        }
        String keyword = StrUtil.trim(pushMessageform.getPageForm().getKeyword());
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        queryWrapper.and(wrapper -> wrapper
                .like(EntityUtils.toDbName(PushMessageItem::getMsgId), keyword)
                .or().like(EntityUtils.toDbName(PushMessageItem::getRecUserId), keyword)
                .or().like(EntityUtils.toDbName(PushMessageItem::getSendUserId), keyword)
                .or().apply(
                        EntityUtils.toDbName(PushMessageItem::getMsgBodyId)
                                + " in (select id from push_message_body where title like concat('%', {0}, '%')"
                                + " or content like concat('%', {0}, '%')"
                                + " or template_code like concat('%', {0}, '%'))",
                        keyword));
    }
}
