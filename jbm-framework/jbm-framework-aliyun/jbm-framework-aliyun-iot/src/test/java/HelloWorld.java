import cn.hutool.core.io.resource.ResourceUtil;
import com.aliyun.alink.apiclient.CommonRequest;
import com.aliyun.alink.apiclient.CommonResponse;
import com.aliyun.alink.apiclient.IoTCallback;
import com.aliyun.alink.apiclient.threadpool.ThreadPool;
import com.aliyun.alink.apiclient.utils.StringUtils;
import com.aliyun.alink.dm.api.DeviceInfo;
import com.aliyun.alink.dm.api.InitResult;
import com.aliyun.alink.dm.api.IoTApiClientConfig;
import com.aliyun.alink.dm.model.ResponseModel;
import com.aliyun.alink.linkkit.api.ILinkKitConnectListener;
import com.aliyun.alink.linkkit.api.IoTMqttClientConfig;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linkkit.api.LinkKitInitParams;
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.ALog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jbm.framework.aliyun.iot.test.sample.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HelloWorld {
    private static final String TAG = "HelloWorld";

    private String pk, dn;
    private ThingSample thingTestManager = null;


    public static void main(String[] args) {
        ALog.d(TAG, "Hello world!");
        ALog.setLevel(ALog.LEVEL_DEBUG);
        HelloWorld manager = new HelloWorld();
        ALog.d(TAG, "args=" + Arrays.toString(args));
        System.out.println(ResourceUtil.getResource("device_id.json"));
        String diPath = ResourceUtil.getResource("device_id.json").getPath();
        String deviceInfo = FileUtils.readFile(diPath);
        if (deviceInfo == null) {
            ALog.e(TAG, "main - need device info path.");
            return;
        }
        Gson mGson = new Gson();
        DeviceInfoData deviceInfoData = mGson.fromJson(deviceInfo, DeviceInfoData.class);
        if (deviceInfoData == null) {
            ALog.e(TAG, "main - deviceInfo format error.");
            return;
        }
        if (StringUtils.isEmptyString(deviceInfoData.deviceSecret)) {
            manager.deviceRegister(deviceInfoData);
            ALog.d(TAG, "测试一型一密动态注册，只测试动态注册");
            return;
        }
        ALog.d(TAG, "测试一机一密和物模型");
        manager.init(deviceInfoData);
    }


    public void init(final DeviceInfoData deviceInfoData) {
        this.pk = deviceInfoData.productKey;
        this.dn = deviceInfoData.deviceName;
        LinkKitInitParams params = new LinkKitInitParams();
        /**
         * 设置 Mqtt 初始化参数
         */
        IoTMqttClientConfig config = new IoTMqttClientConfig();
        config.productKey = deviceInfoData.productKey;
        config.deviceName = deviceInfoData.deviceName;
        config.deviceSecret = deviceInfoData.deviceSecret;
        config.channelHost = pk + ".iot-as-mqtt." + deviceInfoData.region + ".aliyuncs.com:1883";
        /**
         * 是否接受离线消息
         * 对应 mqtt 的 cleanSession 字段
         */
        config.receiveOfflineMsg = false;
        params.mqttClientConfig = config;

        /**
         * 设置初始化三元组信息，用户传入
         */
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.productKey = pk;
        deviceInfo.deviceName = dn;
        deviceInfo.deviceSecret = deviceInfoData.deviceSecret;

        params.deviceInfo = deviceInfo;

        /**
         * 设置设备当前的初始状态值，属性需要和云端创建的物模型属性一致
         * 如果这里什么属性都不填，物模型就没有当前设备相关属性的初始值。
         * 用户调用物模型上报接口之后，物模型会有相关数据缓存。
         */
        Map<String, ValueWrapper> propertyValues = new HashMap<String, ValueWrapper>();
        // 示例
        // propertyValues.put("LightSwitch", new ValueWrapper.BooleanValueWrapper(0));
        params.propertyValues = propertyValues;
        params.fmVersion = "1.0.2";

        thingTestManager = new ThingSample(pk, dn);
        LinkKit.getInstance().init(params, new ILinkKitConnectListener() {
            public void onError(AError aError) {
                ALog.e(TAG, "Init Error error=" + aError);
            }

            public void onInitDone(InitResult initResult) {
                ALog.i(TAG, "onInitDone result=" + initResult);
                executeScheduler(deviceInfoData);
            }
        });
    }

    boolean testDeinit = false;

    /**
     * 定时执行
     *
     * @param deviceInfoData
     */
    public void executeScheduler(DeviceInfoData deviceInfoData) {
        thingTestManager.readData(ResourceUtil.getResource("test_case.json").getPath());
        testMqtt();
        testLabel();
        testCota();
        // 测试网关子设备管理功能，高级版功能
        testGateway(deviceInfoData);

        // 测试获取设备影子，基础版功能，注意不能与高级版共用
//        testDeviceShadow();
//        // test deinit
//        if (!testDeinit) {
//            testDeinit = true;
//            try {
//                Thread.sleep(5*1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//           deinit();
//            HelloWorld helloWorld = new HelloWorld();
//            helloWorld.init(deviceInfoData);
//        }
//         定时上报
        ThreadPool.scheduleAtFixedRate(new Runnable() {
            public void run() {
                thingTestManager.report();
            }
        }, 3, 5, TimeUnit.SECONDS);
    }

    private void testDeviceShadow() {
        DeviceShadowSample sample = new DeviceShadowSample();
        try {
            sample.listenDownStream();
            sample.shadowGet();
            try {
                Thread.sleep(5 * 1000);
            } catch (Exception e) {

            }
            testMqtt();
            // 异步操作，注意别和删除操作一起执行，不能保持时序
            sample.shadowUpdate();

            // 异步操作，注意别和更新一起执行
//            jbm.framework.aliyun.iot.sample.shadowDelete();
            // 异步操作，
//            jbm.framework.aliyun.iot.sample.shadowDeleteAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        jbm.framework.aliyun.iot.sample.shadowDelete();
    }


    /**
     * 动态注册示例代码
     * 1.现在云端创建产品和设备；
     * 2.在云端开启动态注册；
     * 3.填入pk、dn、ps；
     * 4.调用该方法；
     * 5.拿到deviceSecret返回之后 调初始化建联；
     */
    public void deviceRegister(DeviceInfo deviceInfo) {
        LinkKitInitParams params = new LinkKitInitParams();
        IoTMqttClientConfig config = new IoTMqttClientConfig();
        config.productKey = deviceInfo.productKey;
        config.deviceName = deviceInfo.deviceName;

        params.mqttClientConfig = config;
        params.connectConfig = new IoTApiClientConfig();

        params.deviceInfo = deviceInfo;

        final CommonRequest request = new CommonRequest();
        request.setPath("/auth/register/device");
        LinkKit.getInstance().deviceRegister(params, request, new IoTCallback() {
            public void onFailure(CommonRequest commonRequest, Exception e) {
                ALog.e(TAG, "动态注册失败 " + e);
            }

            public void onResponse(CommonRequest commonRequest, CommonResponse commonResponse) {
                if (commonResponse == null || StringUtils.isEmptyString(commonResponse.getData())) {
                    ALog.e(TAG, "动态注册失败 response=null");
                    return;
                }
                try {
                    ResponseModel<Map<String, String>> response = new Gson().fromJson(commonResponse.getData(), new TypeToken<ResponseModel<Map<String, String>>>() {
                    }.getType());
                    if (response != null && "200".equals(response.code)) {
                        ALog.d(TAG, "register success " + (commonResponse == null ? "" : commonResponse.getData()));
                        /**  获取 deviceSecret, 存储到本地，然后执行初始化建联
                         * 这个流程只能走一次，获取到 secret 之后，下次启动需要读取本地存储的三元组，
                         * 直接执行初始化建联，不可以再走动态初始化
                         */
                        // deviceSecret = response.result.get("deviceSecret");
                        // init(pk,dn,ds);
                        return;
                    }
                } catch (Exception e) {

                }
                ALog.d(TAG, "register fail " + commonResponse.getData());
            }
        });
    }

    private void deinit() {
        LinkKit.getInstance().deinit();
    }

    /**
     * 测试 Mqtt 基础topic封装
     * 发布
     * 订阅
     * 取消订阅
     * 注册资源监听，一般用于服务
     */
    private void testMqtt() {
        MqttSample sample = new MqttSample(pk, dn);
        sample.publish();
        sample.subscribe();
        sample.unSubscribe();
        sample.registerResource();
    }

    /**
     * 测试 COTA 远程配置
     */
    private void testCota() {
        COTASample sample = new COTASample(pk, dn);
        // 监听云端 COTA 下行数据更新
        sample.setCOTAChangeListener();
        // 获取 COTA 更新
        sample.cOTAGet();
    }

    /**
     * 标签测试
     */
    private void testLabel() {
        LabelSample sample = new LabelSample(pk, dn);
        // 测试标签更新
        sample.labelUpdate();
        // 测试标签删除
//        jbm.framework.aliyun.iot.sample.labelDelete();
    }

    /**
     * @param deviceInfoData 网关测试
     */
    private void testGateway(DeviceInfoData deviceInfoData) {
        GatewaySample sample = new GatewaySample(pk, dn, deviceInfoData.subDevice);
        sample.getSubDevices();
        // 注册 + 添加 + 登录 + 上报
        sample.subdevRegister();

        ThreadPool.scheduleAtFixedRate(new Runnable() {
            public void run() {
                sample.testSubdevThing();
            }
        }, 3, 5, TimeUnit.SECONDS);

//        try {
//            Thread.sleep(10*1000);
//            // 测试下线 + 删除
//            jbm.framework.aliyun.iot.sample.subDevOffline();
//        } catch (Exception e){
//            e.printStackTrace();
//        }
    }
}
