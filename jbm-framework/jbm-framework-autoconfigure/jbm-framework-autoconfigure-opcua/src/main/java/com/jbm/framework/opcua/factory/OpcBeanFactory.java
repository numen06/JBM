package com.jbm.framework.opcua.factory;

import cn.hutool.aop.proxy.ProxyFactory;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.jbm.framework.opcua.OpcUaTemplate;
import com.jbm.framework.opcua.annotation.OpcUaHeartBeat;
import com.jbm.framework.opcua.annotation.OpcUaReadField;
import com.jbm.framework.opcua.annotation.OpcUaWriteField;
import com.jbm.framework.opcua.attribute.OpcBean;
import com.jbm.framework.opcua.event.PointChangeEvent;
import com.jbm.framework.opcua.interceptor.PointChangeInterceptor;
import com.jbm.framework.opcua.util.ReflectUtils;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;

/**
 * 用于创建OPC UA读写Bean的工厂类
 *
 * @author fanscat
 * @createTime 2022/10/31 18:46
 */
public class OpcBeanFactory {

    /**
     * 获取OPC UA的读写Bean
     *
     * @param applicationContext 应用程序上下文
     * @param device             设备号
     * @param clazz              读写Bean的类型，需要实现OpcBean
     * @param <T>                读写Bean的类型
     * @return OPC UA的读写Bean
     * @throws Exception 获取时发生异常
     */
    public static <T extends OpcBean> T get(ApplicationContext applicationContext, String device, Class<T> clazz) throws Exception {
        OpcUaTemplate opcUaTemplate = applicationContext.getBean(OpcUaTemplate.class);
        if (ObjectUtil.isEmpty(opcUaTemplate.getOpcBean(device))) {
            OpcBeanFactory.initOpcBean(opcUaTemplate, device, clazz);
        }
        return opcUaTemplate.getOpcBean(device);
    }

    /**
     * 初始化OPC UA的读写Bean
     *
     * @param opcUaTemplate OPC UA读写类
     * @param device        设备号
     * @param clazz         读写Bean的类型，需要实现OpcBean
     * @param <T>           读写Bean的类型
     * @throws Exception 初始化时发生异常
     */
    private static <T extends OpcBean> void initOpcBean(OpcUaTemplate opcUaTemplate, String device, Class<T> clazz) throws Exception {
        T opcBean = ProxyFactory.createProxy(ReflectUtil.newInstance(clazz), new PointChangeInterceptor(device, opcUaTemplate));
        for (Field field : ReflectUtil.getFields(clazz)) {
            if (field.isAnnotationPresent(OpcUaWriteField.class)) {
                ReflectUtil.setFieldValue(opcBean, field, opcUaTemplate.readItem(device, ReflectUtils.getWriteFieldAlias(field)));
            } else if (field.isAnnotationPresent(OpcUaHeartBeat.class) || field.isAnnotationPresent(OpcUaReadField.class)) {
                String pointAlias = ReflectUtils.getReadFieldAlias(field);
                ReflectUtil.setFieldValue(opcBean, field, opcUaTemplate.readItem(device, pointAlias));
                opcUaTemplate.subscribeItem(device, new PointChangeEvent(opcBean, device, pointAlias));
            }
        }
        opcUaTemplate.setOpcBean(device, opcBean);
    }

}
