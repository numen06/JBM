package com.jbm.framework.opcua.interceptor;

import cn.hutool.aop.aspects.SimpleAspect;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.opcua.OpcUaTemplate;
import com.jbm.framework.opcua.annotation.OpcUaHeartBeat;
import com.jbm.framework.opcua.util.ReflectUtils;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.Interceptor;

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
        String column = StrUtil.getGeneralField(method.getName());
        if (StrUtil.isEmpty(column)) {
            return super.after(target, method, args, returnVal);
        }
        String pointAlias = ReflectUtils.getWriteFieldAlias(target, column);
        if (ObjectUtil.isEmpty(pointAlias)) {
            return super.after(target, method, args, returnVal);
        }
        Object obj = ReflectUtil.getFieldValue(target, column);
        try {
            if (!ReflectUtil.getField(target.getClass(), column).isAnnotationPresent(OpcUaHeartBeat.class)) {
                // 心跳点位读写频率太高，输出日志时排除心跳
                log.info("设备[{}]点位[{}]写入值[{}]", device, pointAlias, obj);
            }
            this.opcUaTemplate.writeItem(device, pointAlias, obj);
        } catch (Exception var3) {
            log.error("设备[{}]点位[{}]写入值[{}]失败", device, pointAlias, obj, var3);
        }
        return super.after(target, method, args, returnVal);
    }
}
