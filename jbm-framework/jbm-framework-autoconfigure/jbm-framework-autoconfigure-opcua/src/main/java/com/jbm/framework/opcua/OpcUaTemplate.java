package com.jbm.framework.opcua;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.opcua.attribute.OpcBean;
import com.jbm.framework.opcua.attribute.OpcPoint;
import com.jbm.framework.opcua.attribute.OpcPointsRead;
import com.jbm.framework.opcua.attribute.ValueType;
import com.jbm.framework.opcua.event.PointSubscribeEvent;
import com.jbm.framework.opcua.event.ValueChanageEvent;
import com.jbm.framework.opcua.key.KeyLoader;
import com.jbm.framework.opcua.listener.GuardSubscriptionListener;
import com.jbm.framework.opcua.util.DriverUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

@Service
@Slf4j
public class OpcUaTemplate {

    private Map<String, OpcUaClientBean> clientMap = new ConcurrentHashMap<>(16);

//    private Map<String, List<ValueChanageEvent>> nodeEvents = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;


    /**
     * tianjia
     *
     * @param opcUaClientBean
     */
    public synchronized void addClient(OpcUaClientBean opcUaClientBean) {
        this.clientMap.put(opcUaClientBean.getDeviceId(), opcUaClientBean);
        OpcUaClient opcUaClient = this.getOpcUaClient(opcUaClientBean.getDeviceId(), opcUaClientBean.getOpcUaSource());
        opcUaClientBean.setOpcUaClient(opcUaClient);
    }

    public <T extends OpcBean> T getOpcBean(String deviceId) {
        return (T) this.clientMap.get(deviceId).getOpcBean();
    }

    public <T extends OpcBean> void setOpcBean(String deviceId, T opcBean) {
        this.clientMap.get(deviceId).setOpcBean(opcBean);
    }

    public synchronized void removeClient(String deviceId) {
        if (!this.clientMap.containsKey(deviceId)) {
            return;
        }
        OpcUaClientBean opcUaClientBean = this.clientMap.get(deviceId);
        try {
            opcUaClientBean.getOpcUaClient().disconnect();
        } catch (Exception e) {
            log.error("断开客户端[{}]连接失败", deviceId, e);
        } finally {
            this.clientMap.remove(deviceId);
        }
    }


    public void loadClients(Map<String, OpcUaSource> opcUaSourceMap) {
        // 避免某个客户端连接不上时，所有客户端都不初始化问题
        for (String deviceId : opcUaSourceMap.keySet()) {
            try {
                OpcUaSource source = opcUaSourceMap.get(deviceId);
                OpcUaClientBean opcUaClientBean = new OpcUaClientBean();
                opcUaClientBean.setOpcUaSource(source);
                opcUaClientBean.setPoints(this.loadPoints(source));
                opcUaClientBean.setDeviceId(deviceId);
                this.addClient(opcUaClientBean);
            } catch (Exception e) {
                log.error("读取OPC UA设备失败", e);
//            throw e;
            }
        }
//        try {
//            for (String deviceId : opcUaSourceMap.keySet()) {
//                OpcUaSource source = opcUaSourceMap.get(deviceId);
//                OpcUaClient opcUaClient = this.getOpcUaClient(deviceId, source);
//                OpcUaClientBean opcUaClientBean = new OpcUaClientBean();
//                opcUaClientBean.setOpcUaSource(source);
//                opcUaClientBean.setPoints(this.loadPoints(source));
//                opcUaClientBean.setDeviceId(deviceId);
//                opcUaClientBean.setOpcUaClient(opcUaClient);
//                this.addClient(opcUaClientBean);
//            }
//        } catch (Exception e) {
//            log.error("读取OPCUA设备失败", e);
////            throw e;
//        }
    }

    public Map<String, OpcPoint> loadPoints(OpcUaSource opcUaSource) {
        if (StrUtil.isNotBlank(opcUaSource.getPointFile())) {
            OpcPointsRead opcPointsRead = new OpcPointsRead();
            return opcPointsRead.readPoints(opcUaSource.getPointFile(), opcUaSource.getCarryQuote());
        }
        return Maps.newConcurrentMap();
    }

    public Boolean hasClient(String deviceId) {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        if (ObjectUtil.isEmpty(opcUaClientBean)) {
            return false;
        }
        return ObjectUtil.isNotEmpty(opcUaClientBean.getOpcUaClient());
    }

    public List<String> getDeviceIds() {
        return Lists.newArrayList(clientMap.keySet());
    }

    public OpcUaClient getOpcUaClient(String deviceId) {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        if (ObjectUtil.isEmpty(opcUaClientBean.getOpcUaClient())) {
            // OPC UA客户端不存在的情况下尝试重新注册
            this.addClient(opcUaClientBean);
        }
        return opcUaClientBean.getOpcUaClient();
    }

    /**
     * Get Opc Ua Client
     *
     * @param deviceId   Device Id
     * @param driverInfo Driver Info
     * @return OpcUaClient
     * @throws UaException UaException
     */
    public OpcUaClient getOpcUaClient(String deviceId, OpcUaSource driverInfo) {
        if (clientMap.containsKey(deviceId) && clientMap.get(deviceId).getOpcUaClient() != null) {
            return clientMap.get(deviceId).getOpcUaClient();
        }
        try {
            OpcUaSource.OpcUaSecurityPolicy securityPolicy = driverInfo.getSecurityPolicy();

            MessageSecurityMode mode = securityPolicy != null ? ObjectUtil.defaultIfNull(securityPolicy.getMode(), MessageSecurityMode.None) : MessageSecurityMode.None;
            SecurityPolicy policy = securityPolicy != null ? ObjectUtil.defaultIfNull(securityPolicy.getPolicy(), SecurityPolicy.None) : SecurityPolicy.None;

            List<EndpointDescription> remoteEndpoints = DiscoveryClient.getEndpoints(driverInfo.getUrl()).get();
            Optional<EndpointDescription> optional = remoteEndpoints.stream().filter(item -> StrUtil.equals(item.getSecurityPolicyUri(), policy.getUri())).findFirst();
            EndpointDescription configPoint = EndpointUtil.updateUrl(optional.orElseThrow(() -> new ServiceException("not matching endpoint")), driverInfo.getHost(), driverInfo.getPort());

            OpcUaClientConfigBuilder clientConfigBuilder = new OpcUaClientConfigBuilder().setEndpoint(configPoint)
                    .setKeepAliveInterval(uint(3000)).setRequestTimeout(uint(5000));

            if (ObjectUtil.equals(mode, MessageSecurityMode.Sign) && securityPolicy != null) {
                KeyLoader loader = new KeyLoader().load(Paths.get(FileUtil.getTmpDirPath()));
                clientConfigBuilder.setIdentityProvider(new UsernameProvider(securityPolicy.getUsername(), securityPolicy.getPassword()))
                        .setKeyPair(loader.getClientKeyPair())
                        .setCertificate(loader.getClientCertificate())
                        .setApplicationUri(KeyLoader.getApplicationUri());
            } else {
                clientConfigBuilder.setIdentityProvider(new AnonymousProvider());
            }

            OpcUaClient opcUaClient = OpcUaClient.create(driverInfo.getUrl(), endpoints -> optional, configBuilder -> clientConfigBuilder.build());
            opcUaClient.getSubscriptionManager().addSubscriptionListener(new GuardSubscriptionListener(this, clientMap.get(deviceId)));
            return opcUaClient;
        } catch (Exception e) {
            log.error("{} get opc ua client error", deviceId, e);
        }
        return null;
    }

    public String readItem(String deviceId, String pointName, long timeout) throws Exception {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        OpcPoint point = opcUaClientBean.findPoint(pointName);
        if (ObjectUtil.isEmpty(point)) {
            return null;
        }
        return this.readItem(deviceId, opcUaClientBean.getNodeId(pointName), timeout);
    }

    public <T> T readItem(String deviceId, String pointName, Class<T> dataType) throws Exception {
        return JSON.parseObject(this.readItem(deviceId, pointName), dataType);
    }

    public String readItem(String deviceId, String pointName) throws Exception {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        OpcPoint point = opcUaClientBean.findPoint(pointName);
        if (ObjectUtil.isEmpty(point)) {
            return null;
        }
        return this.readItem(deviceId, opcUaClientBean.getNodeId(pointName), 3L);
    }

    public String readItem(String deviceId, OpcPoint point) throws Exception {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        return this.readItem(deviceId, opcUaClientBean.getNodeId(point.getAlias()), 3L);
    }

    public String readItem(String deviceId, NodeId nodeId, long timeout) throws Exception {
        CompletableFuture<String> value = new CompletableFuture<>();
        OpcUaClient client = getOpcUaClient(deviceId);
        log.debug("start read point(ns={};s={})", nodeId.getNamespaceIndex(), nodeId.getIdentifier());
        client.connect().get();
        client.readValue(0.0, TimestampsToReturn.Both, nodeId).thenAccept(dataValue -> {
            try {
                if (ArrayUtil.isArray(dataValue.getValue().getValue())) {
                    value.complete(ArrayUtil.toString(dataValue.getValue().getValue()));
                }
                value.complete(StrUtil.toStringOrNull(dataValue.getValue().getValue()));
            } catch (Exception e) {
                log.error("accept point(ns={};s={}) value error", nodeId.getNamespaceIndex(), nodeId.getIdentifier(), e);
            }
        });
        String rawValue = value.get(timeout, TimeUnit.SECONDS);
        log.debug("end read point(ns={};s={}) value: {}", nodeId.getNamespaceIndex(), nodeId.getIdentifier(), rawValue);
        return rawValue;

    }

    public void writeItem(String deviceId, String pointName, Object value) throws Exception {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        OpcPoint point = opcUaClientBean.findPoint(pointName);
        if (ObjectUtil.isNotEmpty(point)) {
            point.setValue(value);
            this.writeItem(deviceId, opcUaClientBean.getNodeId(point.getAlias()), this.convertData(point));
        }
    }

    public void writeItem(String deviceId, OpcPoint point) throws Exception {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        this.writeItem(deviceId, opcUaClientBean.getNodeId(point.getAlias()), this.convertData(point));
    }

    public void writeItem(String deviceId, NodeId nodeId, DataValue dataValue) {
        OpcUaClient client;
        try {
            log.debug("write point(ns={};s={})", nodeId.getNamespaceIndex(), nodeId.getIdentifier());
            client = getOpcUaClient(deviceId);
            client.connect().get();
            StatusCode statusCode = StatusCode.GOOD;
            statusCode = client.writeValue(nodeId, dataValue).get();
            if (!statusCode.isGood()) {
                throw new RuntimeException(statusCode.toString());
            }
        } catch (Exception e) {
            log.error("Opc Ua Point Write Error", e);
        }
    }

    public <T extends ValueChanageEvent> void subscribeItem(String deviceId, String pointName, Class<T> callBackEvent) {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        OpcPoint opcPoint = opcUaClientBean.findPoint(pointName);
        ValueChanageEvent valueChanageEvent = ReflectUtil.newInstance(null, null, null);
        this.subscribeItem(deviceId, opcPoint, valueChanageEvent);
    }

    public <T extends PointSubscribeEvent> void subscribeItem(String deviceId, T pointSubscribeEvent) {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        OpcPoint opcPoint = opcUaClientBean.findPoint(pointSubscribeEvent.getOpcPoint().getAlias());
        this.subscribeItem(deviceId, opcPoint, pointSubscribeEvent);
    }


//    public <T extends ValueChanageEvent> void putEvent(String deviceId, NodeId point, Class<T> callBackEvent) {
//        String key = String.format("%s-%s-%s", deviceId, point.getNamespaceIndex(), point.getIdentifier());
//        if (this.nodeEvents.containsKey(key)) {
//            this.nodeEvents.get(key).add(callBackEvent);
//        } else {
//            this.nodeEvents.put(key, Lists.newArrayList(callBackEvent));
//        }
//    }

    public <T extends ValueChanageEvent> void putEvent(OpcUaClientBean opcUaClientBean, OpcPoint point, T callBackEvent) {
        if (!opcUaClientBean.getSubscriptionPoints().containsKey(point.getAlias())) {
            opcUaClientBean.getSubscriptionPoints().put(point.getAlias(), callBackEvent);
        }
    }

    /**
     * 创建监听器
     *
     * @param client
     * @return
     */
    public UaSubscription getSubscription(OpcUaClient client) throws ExecutionException, InterruptedException {
        UaSubscription subscription = CollUtil.getFirst(client.getSubscriptionManager().getSubscriptions());
        if (subscription == null) {
            subscription = client.getSubscriptionManager().createSubscription(1000.0).get();
        }
        return subscription;
    }


    /**
     * 订阅节点
     */
    public <T extends ValueChanageEvent> void subscribeItem(String deviceId, OpcPoint opcPoint, T callBackEvent) {
        OpcUaClient client;
        try {
            log.info("OPCUA订阅点位:{}", opcPoint.getAlias());
            OpcUaClientBean opcUaClientBean = this.clientMap.get(deviceId);
            client = getOpcUaClient(opcUaClientBean.getDeviceId());
            client.connect().get();
            List<UaMonitoredItem> items = this.createItemMonitored(opcUaClientBean, opcPoint);
            //循环设置回调事件
            this.putEvent(opcUaClientBean, opcPoint, callBackEvent);
        } catch (Exception e) {
            log.error("Opc Ua Point Write Error", e);
        }
    }

    private List<UaMonitoredItem> createItemMonitored(OpcUaClientBean opcUaClientBean, OpcPoint opcPoint) throws ExecutionException, InterruptedException {
        NodeId nodeId = opcUaClientBean.getNodeId(opcPoint.getAlias());
        //创建发布间隔1000ms的订阅对象
        UaSubscription subscription = this.getSubscription(opcUaClientBean.getOpcUaClient());
        MonitoringParameters parameters = new MonitoringParameters(
                uint(subscription.getMonitoredItems().size() + 1),
                1000.0,
                null,
                uint(10),
                true
        );
        List<MonitoredItemCreateRequest> requests = Lists.newArrayList();
        ReadValueId readValueId = new ReadValueId(nodeId, AttributeId.Value.uid(), null, null);
        //创建监控item, 第一个为Reporting mode
        MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
        requests.add(request);
        List<UaMonitoredItem> items = subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                (item, id) -> item.setValueConsumer(new UaMonitoredItem.ValueConsumer() {
                    @Override
                    public void onValueArrived(UaMonitoredItem item, DataValue value) {
                        try {
                            log.debug("OPC数据变化回调:subscription value received: item={}, value={}", item.getReadValueId().getNodeId(), value.getValue());
                            ValueChanageEvent valueChanageEvent = opcUaClientBean.getSubscriptionPoints().get(opcPoint.getAlias());
                            // 判断数据是否发生变化后再执行回调事件
                            if (valueChanageEvent.putData(item, value)) {
                                applicationContext.publishEvent(valueChanageEvent);
                            }
                        } catch (Exception e) {

                        }
                    }
                })
        ).get();
        log.info("添加监听:[{}]到监听器[{}]监听数量:{}", nodeId, subscription.getSubscriptionId(), subscription.getMonitoredItems().size());
        return items;
    }


    private DataValue convertData(OpcPoint point) {
        ValueType valueType = ValueType.valueOf(point.getDataType().toUpperCase());
        return this.convertData(valueType, point.getValue());
    }

    /**
     * 转换数据类型
     *
     * @param valueType
     * @param value
     * @return
     */
    private DataValue convertData(ValueType valueType, Object value) {
        switch (valueType) {
            case UBYTE:
                return buildDataValue(UByte.valueOf(value.toString()));
            case USHORT:
                return buildDataValue(UShort.valueOf(value.toString()));
            default:
                return buildDataValue(DriverUtils.value(valueType, value.toString()));
        }
    }


    private DataValue buildDataValue(Object val) {
        return new DataValue(new Variant(val), null, null);
    }

}
