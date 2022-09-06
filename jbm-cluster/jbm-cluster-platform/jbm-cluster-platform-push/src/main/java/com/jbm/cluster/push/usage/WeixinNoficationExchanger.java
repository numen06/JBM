package com.jbm.cluster.push.usage;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.entitys.message.WeixinNotification;
import com.jbm.cluster.api.entitys.message.weixin.WxMpTemplateData;
import com.jbm.cluster.api.model.push.PushCallback;
import com.jbm.cluster.api.service.fegin.client.BaseUserServiceClient;
import com.jbm.cluster.api.service.fegin.weixin.clinet.WeixinMpClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wesley.zhang
 * @date 2018-3-27
 **/
public class WeixinNoficationExchanger extends BaseNotificationExchanger<WeixinNotification> {


    @Autowired
    private BaseUserServiceClient baseUserServiceClient;

    @Autowired
    private WeixinMpClient weixinMpClient;

    public WeixinNoficationExchanger() {
    }

    @Override
    public PushCallback apply(WeixinNotification weixinNotification) {
        weixinMpClient.sendTemplateMsg(weixinNotification);
        return this.success(weixinNotification);
    }

    @Override
    public WeixinNotification build(PushMessageBody pushMessageBody, PushMessageItem pushMessageItem) {
        WeixinNotification weixinNotification = new WeixinNotification();
        baseUserServiceClient.getUserAccounts(pushMessageItem.getSendUserId()).action(baseAccounts -> {
            baseAccounts.forEach(baseAccount -> {
                if (PushWay.wechat.toString().equalsIgnoreCase(baseAccount.getAccountType())) {
                    weixinNotification.setToUser(baseAccount.getAccount());
                    weixinNotification.setTemplateId(pushMessageBody.getTemplateCode());
                    List<WxMpTemplateData> list = new ArrayList<>();
                    pushMessageBody.getExtend().forEach((key, val) -> {
                        list.add(new WxMpTemplateData(key, StrUtil.toString(val)));
                    });
                    weixinNotification.setData(list);
                }
            });
        });
        return weixinNotification;
    }


}
