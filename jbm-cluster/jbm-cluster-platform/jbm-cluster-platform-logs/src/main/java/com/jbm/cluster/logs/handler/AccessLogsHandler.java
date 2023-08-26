package com.jbm.cluster.logs.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.jbm.cluster.api.model.entity.BaseUser;
import com.jbm.cluster.common.configuration.JbmClusterProperties;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.service.GatewayLogsService;
import com.jbm.cluster.logs.utils.AddressUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.util.statistics.CountWithTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * mq消息接收者
 *
 * @author wesley.zhang
 */
@Service
@Slf4j
public class AccessLogsHandler {

    /**
     * 临时存放减少io
     */
    @Autowired
    private GatewayLogsService gatewayLogsService;

    private CountWithTime countWithTime = new CountWithTime() {


        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.MINUTES);
        }

        @Override
        public void print() {
            log.info("最近1分钟处理日志:{}", this.getAvg());
        }
    };

    @Autowired
    private JbmClusterProperties jbmClusterProperties;

    LoadingCache<String, BaseUser> baseUserLoadingCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(new CacheLoader<String, BaseUser>() {
        @Override
        public BaseUser load(String token) throws Exception {
            HttpRequest httpRequest = HttpRequest.get(jbmClusterProperties.getUserInfoUri()).bearerAuth(token);
            String body = httpRequest.execute().body();
            BaseUser baseUser = JSON.parseObject(body, new TypeReference<ResultBody<BaseUser>>() {
            }).getResult();
            if (baseUser.getUserId() == null) {
                throw new NullPointerException();
            }
            return baseUser;
        }
    });


    private BaseUser getUserByToken(GatewayLogs logs) {
        if (StrUtil.isBlank(logs.getHeaders())) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(logs.getHeaders());
            String token = StrUtil.removePrefixIgnoreCase(jsonObject.getString("Authorization"), "Bearer ");
            if (StrUtil.isBlank(token)) {
                return null;
            }
            return baseUserLoadingCache.get(token);
        } catch (Exception e) {
            return null;
        }
    }

    private AntPathMatcher pathMatch = new AntPathMatcher();

    private Set<String> pathIgnores = Sets.newHashSet("/logs/**", "/admin/**", "/**/v2/api-docs", "/base/baseDic/getDicMap");

    /**
     * 接收访问日志
     *
     * @param gatewayLogInfoJson
     */
    @RabbitListener(queues = QueueConstants.QUEUE_ACCESS_LOGS)
    public void accessLogsQueue(@Payload String gatewayLogInfoJson) {
        try {
            countWithTime.add();
            if (StrUtil.isNotBlank(gatewayLogInfoJson)) {
                GatewayLogs logs = JSON.parseObject(gatewayLogInfoJson, GatewayLogs.class);
                if (ObjectUtil.isNotEmpty(logs)) {
                    if (StrUtil.isNotBlank(logs.getIp())) {
                        logs.setRegion(AddressUtils.getIPRegion(logs.getIp()));
                    }
                    for (String path : pathIgnores) {
                        if (pathMatch.match(path, logs.getPath())) {
                            return;
                        }
                    }
                    //过滤本应用的查询
                    BaseUser baseUser = getUserByToken(logs);
                    if (ObjectUtil.isNotNull(baseUser)) {
                        logs.setRequestUserId(baseUser.getUserId());
                        logs.setRequestUserRealName(StrUtil.format("{}|{}|{}", baseUser.getUserName(), baseUser.getNickName(), baseUser.getRealName()));
                    }
//                    logs.setUseTime(logs.getResponseTime().getTime() - logs.getRequestTime().getTime());
                    gatewayLogsService.save(logs);
                }
            }
        } catch (Exception e) {
            log.error("日志接收错误:", e);
        }
    }
}
