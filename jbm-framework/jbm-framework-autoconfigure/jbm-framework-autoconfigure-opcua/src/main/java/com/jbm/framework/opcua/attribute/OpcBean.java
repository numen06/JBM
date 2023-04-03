package com.jbm.framework.opcua.attribute;

import com.jbm.framework.opcua.factory.OpcBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * 通过Bean读写OPC UA时需要继承的接口
 * <ol>
 *     搭配注解实现OPC UA读写
 *     <li>{@link com.jbm.framework.opcua.annotation.OpcUaHeartBeat}</li>
 *     <li>{@link com.jbm.framework.opcua.annotation.OpcUaReadField}</li>
 *     <li>{@link com.jbm.framework.opcua.annotation.OpcUaWriteField}</li>
 * </ol>
 * <p>
 * 通过以下形式创建的OPC UA的Bean才支持读写PLC
 * <p>
 * {@link OpcBeanFactory#get(ApplicationContext, String, Class)}
 *
 * @author fanscat
 * @CreateTime 2022/10/31 19:00
 */
public interface OpcBean {

}
