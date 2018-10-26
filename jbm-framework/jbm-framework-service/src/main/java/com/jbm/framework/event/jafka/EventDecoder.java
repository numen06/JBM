package com.jbm.framework.event.jafka;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

import com.sohu.jafka.message.Message;
import com.sohu.jafka.producer.serializer.Decoder;
import com.jbm.framework.event.bean.RemoteEventBean;

/**
 * 事件反序列化集成类
 * 
 * @author wesley
 *
 */
public class EventDecoder implements Decoder<RemoteEventBean> {
	public static final Logger logger = LoggerFactory.getLogger(EventDecoder.class);

	@Override
	public RemoteEventBean toEvent(Message message) {
		RemoteEventBean bean = null;
		try {
			ByteBuffer buf = message.payload();
			byte[] b = new byte[buf.remaining()];
			buf.get(b);
			bean = (RemoteEventBean) SerializationUtils.deserialize(b);
		} catch (Exception e) {
			logger.debug("反序列化远程事件错误", e);
		}
		return bean;
	}
}
