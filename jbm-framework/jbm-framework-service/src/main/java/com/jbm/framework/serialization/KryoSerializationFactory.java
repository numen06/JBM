package com.jbm.framework.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import org.apache.commons.lang.SerializationException;
import org.springframework.context.ApplicationEvent;

import com.esotericsoftware.kryo.io.FastInput;
import com.esotericsoftware.kryo.io.FastOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Lists;
import com.mongodb.DBObject;
import com.jbm.framework.common.SerializationFactory;
import com.jbm.framework.event.bean.RemoteEventBean;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.metadata.usage.bean.AdvMongoBean;
import com.jbm.framework.metadata.usage.bean.BaseMongoBean;
import com.jbm.framework.metadata.usage.bean.PrimaryKey;
import com.jbm.framework.metadata.usage.page.DataPaging;
import com.jbm.framework.metadata.usage.page.PageForm;

/**
 * 高效序列化工具
 * 
 * @author wesley
 *
 */
public class KryoSerializationFactory implements SerializationFactory<Object> {
	private final static Kryox kryo = new Kryox();

	private final static KryoSerializationFactory instance = new KryoSerializationFactory();

	private final List<Class<?>> serializers = Lists.newArrayList();

	private KryoSerializationFactory() {
		super();
		register(EventObject.class);
		register(ApplicationEvent.class);
		register(RemoteEventBean.class);
		register(PageForm.class);
		register(DataPaging.class);
		register(PrimaryKey.class);
		register(DBObject.class);
		register(BaseMongoBean.class);
		register(AdvMongoBean.class);
		register(ResultForm.class);
	}

	public static KryoSerializationFactory INSTANCE() {
		return instance;
	}

	public byte[] serialize(final Object obj) throws IOException {
		ByteArrayOutputStream out = null;
		Output output = null;
		try {
			out = new ByteArrayOutputStream();
			output = new FastOutput(out);
			kryo.writeClassAndObject(output, obj);
			return output.toBytes();
		} catch (Exception e) {
			throw new SerializationException(e);
		} finally {
			if (null != out) {
				try {
					out.close();
					out = null;
				} catch (IOException e) {
				}
			}
			if (null != output) {
				output.close();
				output = null;
			}
		}
	}

	public Object deserialize(final byte[] bytes) throws IOException {
		return deserialize(bytes, Object.class, null);
	}

	@Override
	public void register(final Class<?> clazz) {
		serializers.add(clazz);
		kryo.register(clazz);
	}

	public List<Class<?>> regSerializers() {
		return Collections.unmodifiableList(serializers);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E deserialize(final byte[] bytes, Class<E> clazz, E def) {
		if (bytes == null)
			return def;
		Input input = null;
		try {
			input = new FastInput(bytes);
			Object obj = kryo.readClassAndObject(input);
			E temp = (E) obj;
			return temp == null ? def : temp;
		} catch (Exception e) {
			e.printStackTrace();
			return def;
		} finally {
			if (null != input) {
				input.close();
				input = null;
			}
		}
	}

}
