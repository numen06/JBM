package com.jbm.framework.event.jafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

import com.sohu.jafka.message.Message;
import com.sohu.jafka.producer.serializer.Encoder;
import com.jbm.framework.event.bean.RemoteEventBean;

/**
 * 
 * 将时间序列化传输的集成类
 * 
 * @author wesley
 *
 */
public class EventEncoder implements Encoder<RemoteEventBean> {

	public static final Logger logger = LoggerFactory.getLogger(EventEncoder.class);

	@Override
	public Message toMessage(RemoteEventBean event) {
		byte[] data = null;
		try {
			data = SerializationUtils.serialize(event);
		} catch (Exception e) {
			logger.debug("序列化远程事件错误", e);
			return null;
		}
		return new Message(data);
	}

}
