package com.jbm.framework.opcua.factory;

import cn.hutool.aop.ProxyUtil;
import cn.hutool.core.util.ReflectUtil;
import com.jbm.framework.opcua.OpcBean;
import com.jbm.framework.opcua.OpcUaTemplate;
import com.jbm.framework.opcua.event.PointChangeEvent;
import com.jbm.framework.opcua.interceptor.PointChangeInterceptor;
import com.jbm.framework.opcua.util.ReflectUtils;

import java.lang.reflect.Field;

/**
 * @Author fanscat
 * @CreateTime 2022/10/31 18:46
 * @Description
 */
public class OpcBeanFactory {

    public <T extends OpcBean> T get(String device, OpcUaTemplate opcUaTemplate, Class<T> clazz) throws Exception {
        T opcBean = ProxyUtil.proxy(ReflectUtil.newInstance(clazz), new PointChangeInterceptor(device, opcUaTemplate));
        for (Field field : ReflectUtil.getFields(clazz)) {
            String column = ReflectUtils.camelToUnderline(field.getName());
            ReflectUtils.setFieldValue(opcBean, field.getName(), opcUaTemplate.readItem(device, column));
            if (column.indexOf(ReflectUtils.READ) == 0) {
                // 属性名必须[read]开头，才会触发OPC订阅
                opcUaTemplate.subscribeItem(device, new PointChangeEvent(this, device, column, opcBean));
            }
        }
        return opcBean;
    }

}
