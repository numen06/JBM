package com.td.framework.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.caucho.hessian.io.HessianFactory;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.td.framework.common.SerializationFactory;

public class HessianSerializationFactory implements SerializationFactory<Object> {

	private final static HessianSerializationFactory instance = new HessianSerializationFactory();

	private final static HessianFactory factory = new HessianFactory();

	@Override
	public byte[] serialize(Object obj) throws IOException {
		if (obj == null)
			throw new NullPointerException();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			HessianOutput ho = factory.createHessianOutput(os);
			ho.writeObject(obj);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return os.toByteArray();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E deserialize(byte[] bytes, Class<E> clazz, E def) {
		if (bytes == null)
			throw new NullPointerException();
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		try {
			HessianInput hi = factory.createHessianInput(is);
			return (E) hi.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return def;
	}

	public static HessianSerializationFactory INSTANCE() {
		return instance;
	}

	@Override
	public Object deserialize(byte[] bytes) throws IOException {
		return deserialize(bytes, Object.class, null);
	}

	@Override
	public void register(Class<?> clazz) {

	}

}
