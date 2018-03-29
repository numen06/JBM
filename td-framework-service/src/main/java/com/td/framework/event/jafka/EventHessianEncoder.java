package com.td.framework.event.jafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sohu.jafka.message.Message;
import com.sohu.jafka.producer.serializer.Encoder;
import com.td.framework.event.bean.RemoteEventBean;
import com.td.framework.serialization.HessianSerializationFactory;

/**
 * 
 * 将时间序列化传输的集成类
 * 
 * @author wesley
 *
 */
public class EventHessianEncoder implements Encoder<RemoteEventBean> {

	public static final Logger logger = LoggerFactory.getLogger(EventHessianEncoder.class);

	@Override
	public Message toMessage(RemoteEventBean event) {
		byte[] data = null;
		try {
			data = HessianSerializationFactory.INSTANCE().serialize(event);
		} catch (Exception e) {
			logger.debug("序列化远程事件错误", e);
			return null;
		}
		return new Message(data);
	}

}
