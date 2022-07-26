package com.jbm.cluster.center.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.cluster.api.model.dic.JbmDicResource;
import com.jbm.cluster.center.service.BaseDicService;
import com.jbm.framework.dictionary.JbmDictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * mq消息接收者
 *
 * @author wesley.zhang
 */
@Configuration
@Slf4j
public class DictionaryScanHandler {
    @Autowired
    private BaseDicService baseDicService;

    @Bean
    public Function<Flux<Message<JbmDicResource>>, Mono<Void>> dicResource() {
        return flux -> flux.map(message -> {
            this.scanDicResourceQueue(message.getPayload());
            return message;
        }).then();
    }

    /**
     * 接收API资源扫描消息
     */
//    @RabbitListener(queues = QueueConstants.QUEUE_SCAN_DIC_RESOURCE)
    public void scanDicResourceQueue(JbmDicResource jbmDicResource) {
        List<JbmDictionary> jbmDictionaryList = CollUtil.emptyIfNull(jbmDicResource.getJbmDictionaryList());
        StopWatch stopWatch = new StopWatch(StrUtil.format("接受到集群推送的字典,来自服务:{},数量为:{}", jbmDicResource.getServiceId(), jbmDictionaryList.size()));
        stopWatch.start();
        for (JbmDictionary jbmDictionary : jbmDictionaryList) {
            BaseDic baseDicType = this.conventType(jbmDictionary);
            BaseDic baseDic = this.conventDic(baseDicType, jbmDictionary);
            if (ObjectUtil.isEmpty(baseDic)) {
                continue;
            }
            baseDicService.saveEntity(baseDic);
        }
        stopWatch.stop();
        log.info(stopWatch.prettyPrint(TimeUnit.SECONDS));
    }

    private BaseDic conventType(JbmDictionary jbmDictionary) {
        BaseDic dicType = baseDicService.getBaseDicType(jbmDictionary.getType());
        if (ObjectUtil.isEmpty(dicType)) {
            dicType = new BaseDic();
        }
        dicType.setCode(jbmDictionary.getType());
        dicType.setServiceId(jbmDictionary.getApplication());
        dicType.setName(jbmDictionary.getTypeName());
        dicType = baseDicService.saveEntity(dicType);
        return dicType;
    }

    private BaseDic conventDic(BaseDic dicType, JbmDictionary jbmDictionary) {
        if (ObjectUtil.isEmpty(dicType)) {
            return null;
        }
        if (ObjectUtil.isEmpty(dicType.getId())) {
            return null;
        }
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
