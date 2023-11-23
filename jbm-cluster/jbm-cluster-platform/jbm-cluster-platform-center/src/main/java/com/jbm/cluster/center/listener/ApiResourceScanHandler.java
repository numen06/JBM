package com.jbm.cluster.center.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.model.api.JbmApi;
import com.jbm.cluster.api.model.api.JbmApiResource;
import com.jbm.cluster.center.service.BaseApiService;
import com.jbm.cluster.center.service.BaseAuthorityService;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * mq消息接收者
 *
 * @author wesley.zhang
 */
@Configuration
@Slf4j
public class ApiResourceScanHandler {
    private final static String SCAN_API_RESOURCE_KEY_PREFIX = "scan_api_resource:";
    @Autowired
    private BaseApiService baseApiService;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private RedisService redisService;

    @Bean
    public Function<Flux<Message<JbmApiResource>>, Mono<Void>> apiResource() {
        return flux -> flux.map(message -> {
            this.scanApiResourceQueue(message.getPayload());
            return message;
        }).then();
    }

    /**
     * 接收API资源扫描消息
     */
//    @RabbitListener(queues = QueueConstants.QUEUE_SCAN_API_RESOURCE)
    public void scanApiResourceQueue(JbmApiResource jbmApiResource) {
        StopWatch stopWatch = new StopWatch(StrUtil.format("接受到API资源信息,来自服务:{},数量为:{}", jbmApiResource.getServiceId(), CollUtil.emptyIfNull(jbmApiResource.getJbmApiList()).size()));
        stopWatch.start();
        try {
            String key = SCAN_API_RESOURCE_KEY_PREFIX + jbmApiResource.getServiceId();
            Object object = redisService.getCacheObject(key);
//            if (object != null) {
//                // 3分钟内未失效,不再更新资源
//                return;
//            }
            List<String> codes = Lists.newArrayList();
            jbmApiResource.getJbmApiList().parallelStream().forEach(new Consumer<JbmApi>() {
                @Override
                public void accept(JbmApi jbmApi) {
                    try {
                        BaseApi api = new BaseApi();
                        api.setAccessLog(jbmApi.getAccessLog());
                        //复制Bean
                        BeanUtil.copyProperties(jbmApi, api);
                        api.setPath(CollUtil.getFirst(jbmApi.getPaths()));
                        api.setContentType(StrUtil.join(",", jbmApi.getContentTypes()));
                        codes.add(api.getApiCode());
                        BaseApi save = baseApiService.getApi(api.getApiCode());
                        if (save == null) {
                            api.setIsOpen(0);
                            api.setIsPersist(true);
                            baseApiService.addApi(api);
                        } else {
                            api.setApiId(save.getApiId());
                            baseApiService.updateApi(api);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("添加资源error:", e.getMessage());
                    }
                }
            });
            if (CollUtil.isNotEmpty(jbmApiResource.getJbmApiList())) {
                // 清理无效权限数据
                baseAuthorityService.clearInvalidApi(jbmApiResource.getServiceId(), codes);
                // 发送更新API事件
//                restTemplate.refreshGateway();
                // 设置API数量到缓存
//                redisService.setCacheObject(key, codes.size(), 5l, TimeUnit.MINUTES);
            }

        } catch (Exception e) {
            log.error("更新API资源信息错误", e);
        } finally {
            stopWatch.stop();
            log.info(stopWatch.prettyPrint(TimeUnit.SECONDS));
        }
    }

}
