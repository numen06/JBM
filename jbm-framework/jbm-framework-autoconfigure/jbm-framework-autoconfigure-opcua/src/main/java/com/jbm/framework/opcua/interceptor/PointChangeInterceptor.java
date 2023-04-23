package com.jbm.framework.opcua.interceptor;

import cn.hutool.aop.aspects.SimpleAspect;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.opcua.OpcUaTemplate;
import com.jbm.framework.opcua.annotation.OpcUaHeartBeat;
import com.jbm.framework.opcua.util.ReflectUtils;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.Interceptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author fanscat
 * @createTime 2022/10/31 18:47
 */
@Slf4j
public class PointChangeInterceptor extends SimpleAspect implements Interceptor {

    private String device;
    private OpcUaTemplate opcUaTemplate;

    public PointChangeInterceptor(String device, OpcUaTemplate opcUaTemplate) {
        this.device = device;
        this.opcUaTemplate = opcUaTemplate;
    }

    @Override
    public boolean after(Object target, Method method, Object[] args, Object returnVal) {
        if (!ReflectUtil.isGetterOrSetterIgnoreCase(method)) {
            return super.after(target, method, args, returnVal);
        }
        final Field field = ReflectUtils.getField(target, StrUtil.getGeneralField(method.getName()));
        if (StrUtil.equals(method.getName(), StrUtil.genGetter(field.getName()))) {
            // 执行Getter方法时无需写入OPC UA
            return super.after(target, method, args, returnVal);
        }
        String alias = ReflectUtils.getWriteAlias(field);
        if (StrUtil.isBlank(alias)) {
            return super.after(target, method, args, returnVal);
        }
        Object fieldValue = ReflectUtils.getFieldValue(target, field);
        try {
            if (!field.isAnnotationPresent(OpcUaHeartBeat.class)) {
                // 心跳点位读写频率太高，输出日志时排除心跳
                log.info("设备[{}]点位[{}]写入值[{}]", device, alias, fieldValue);
            }
            this.opcUaTemplate.writeItem(device, alias, fieldValue);
        } catch (Exception var3) {
            log.error("设备[{}]点位[{}]写入值[{}]失败", device, alias, fieldValue, var3);
        }
        return super.after(target, method, args, returnVal);
    }

}
