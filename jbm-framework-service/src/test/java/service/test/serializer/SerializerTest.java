package service.test.serializer;

import java.io.IOException;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.event.bean.RemoteEventBean;
import com.jbm.framework.serialization.HessianSerializationFactory;
import com.jbm.framework.serialization.KryoSerializationFactory;

public class SerializerTest {

	public static void main(String[] args) throws IOException {
		byte[] bytes = KryoSerializationFactory.INSTANCE().serialize(new RemoteEventBean("12312"));
		System.out.println(JSON.toJSONString(KryoSerializationFactory.INSTANCE().deserialize(bytes)));

		byte[] bytes2 = HessianSerializationFactory.INSTANCE().serialize(new RemoteEventBean("12312"));
		System.out.println(JSON.toJSONString(HessianSerializationFactory.INSTANCE().deserialize(bytes2)));

	}
}
