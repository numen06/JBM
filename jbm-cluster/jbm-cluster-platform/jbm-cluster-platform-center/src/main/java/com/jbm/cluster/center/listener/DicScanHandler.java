package com.jbm.cluster.center.listener;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.cluster.center.service.BaseDicService;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.common.security.http.OpenRestTemplate;
import com.jbm.framework.dictionary.JbmDictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.List;

/**
 * mq消息接收者
 *
 * @author wesley.zhang
 */
@Configuration
@Slf4j
public class DicScanHandler {
    @Autowired
    private BaseDicService baseDicService;

    /**
     * 接收API资源扫描消息
     */
    @RabbitListener(queues = QueueConstants.QUEUE_SCAN_DIC_RESOURCE)
    public void ScanDicResourceQueue(@Payload List<JbmDictionary> jbmDictionaryList) {
        log.info("接受到集群推送的字典,数量为:{}", jbmDictionaryList.size());
        for (JbmDictionary jbmDictionary : jbmDictionaryList) {
            BaseDic baseDicType = this.conventType(jbmDictionary);
            BaseDic baseDic = this.conventDic(baseDicType, jbmDictionary);
            if (ObjectUtil.isEmpty(baseDic))
               continue;
            baseDicService.saveEntity(baseDic);
        }
    }

    private BaseDic conventType(JbmDictionary jbmDictionary) {
        BaseDic dicType = baseDicService.getBaseDicType(jbmDictionary.getType());
        if (ObjectUtil.isEmpty(dicType))
            dicType = new BaseDic();
        dicType.setCode(jbmDictionary.getType());
        dicType.setServiceId(jbmDictionary.getApplication());
        dicType.setName(jbmDictionary.getTypeName());
        dicType = baseDicService.saveEntity(dicType);
        return dicType;
    }

    private BaseDic conventDic(BaseDic dicType, JbmDictionary jbmDictionary) {
        if (ObjectUtil.isEmpty(dicType))
            return null;
        if (ObjectUtil.isEmpty(dicType.getId()))
            return null;
        BaseDic baseDic = baseDicService.getBaseDic(dicType.getId(), jbmDictionary.getCode());
        if (ObjectUtil.isEmpty(baseDic)) {
            baseDic = new BaseDic();
        }
        //已经存在类型的字典
        baseDic.setParentId(dicType.getId());
        baseDic.setCode(jbmDictionary.getCode());
        baseDic.setServiceId(jbmDictionary.getApplication());
        baseDic.setName(jbmDictionary.getValue());
        return baseDic;
    }


}
