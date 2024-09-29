package jbm.framework.boot.autoconfigure.redis.cache;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * @author welsey
 */
public class JbmKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder sb = new StringBuilder();
        String className = ClassUtil.getClassName(target.getClass(), false);
        String methodName = method.getName();
        String fullName = StrUtil.format("{}.{}", className, methodName);
        sb.append(fullName);
        sb.append(JSONArray.toJSONString(params));
        return sb.toString();
    }

}
