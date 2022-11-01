package com.jbm.framework.opcua.interceptor;

import cn.hutool.aop.aspects.SimpleAspect;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.jbm.framework.opcua.OpcUaTemplate;
import com.jbm.framework.opcua.util.ReflectUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * @Author fanscat
 * @CreateTime 2022/10/31 18:47
 * @Description
 */
@Slf4j
public class PointChangeInterceptor extends SimpleAspect {

    private String device;
    private OpcUaTemplate opcUaTemplate;

    public PointChangeInterceptor(String device, OpcUaTemplate opcUaTemplate) {
        this.device = device;
        this.opcUaTemplate = opcUaTemplate;
    }

    @Override
    public boolean after(Object target, Method method, Object[] args, Object returnVal) {
        String name = method.getName();
        String column = ReflectUtils.firstToLowerCase(name.substring(name.indexOf(ReflectUtils.SET) + 3));
        if (ObjectUtil.isEmpty(ReflectUtil.getField(target.getClass(), column))) {
            return true;
        }
        try {
            if (column.indexOf(ReflectUtils.WRITE) == 0) {
                // 属性名必须[write]开头，才会触发OPC写入
                opcUaTemplate.writeItem(device, ReflectUtils.camelToUnderline(column), ReflectUtils.getFieldValue(target, column));
            }
        } catch (Exception var3) {
            log.error("设备[{}]点位[{}]写入值[{}]失败", device, ReflectUtils.camelToUnderline(column), ReflectUtils.getFieldValue(target, column), var3);
        }
        return super.after(target, method, args, returnVal);
    }
}
