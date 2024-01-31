package jbm.framework.boot.autoconfigure.fastjson;

import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.util.IdentityHashMap;

public class FastJsonSerializeConfig extends SerializeConfig {
//    static {
//        SerializeConfig serializeConfig = SerializeConfig.globalInstance;
//        serializeConfig.put(BigInteger.class, ToStringSerializer.instance);
//        serializeConfig.put(Long.class, ToStringSerializer.instance);
//        serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
//    }

    public FastJsonSerializeConfig() {
        this(IdentityHashMap.DEFAULT_SIZE);
    }

    public FastJsonSerializeConfig(boolean fieldBase) {
        this(IdentityHashMap.DEFAULT_SIZE, fieldBase);
    }

    public FastJsonSerializeConfig(int tableSize) {
        this(tableSize, false);
    }

    public FastJsonSerializeConfig(int tableSize, boolean fieldBase) {
        super(tableSize, fieldBase);
    }

    @Override
    public ObjectSerializer getObjectWriter(Class<?> clazz) {
        return super.getObjectWriter(clazz);
    }

    @Override
    public ObjectSerializer getObjectWriter(Class<?> clazz, boolean create) {
        return super.getObjectWriter(clazz, create);
    }
}
