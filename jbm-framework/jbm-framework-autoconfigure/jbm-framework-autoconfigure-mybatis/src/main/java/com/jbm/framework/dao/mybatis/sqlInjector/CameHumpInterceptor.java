package com.jbm.framework.dao.mybatis.sqlInjector;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;

import java.sql.Statement;
import java.util.*;

@Intercepts(
        @Signature(
                type = ResultSetHandler.class,
                method = "handleResultSets",
                args = {Statement.class})

)
@SuppressWarnings({"unchecked", "rawtypes"})
@Slf4j
public class CameHumpInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
//        log.info("驼峰转换器进行拦截");
        List<Object> list = (List<Object>) invocation.proceed();
        for (Object object : list) {
            //如果结果是Map类型，就对Map的key进行转换
            if (object instanceof Map) {
                processMap((Map) object);
            } else {
                break;
            }
        }
        return list;
    }

    /**
     * 处理map类型
     *
     * @param map
     */
    private void processMap(Map<String, Object> map) {
//        log.info("驼峰转换器进行拦截处理map类型");
        Set<String> keySet = new HashSet<>(map.keySet());
        for (String key : keySet) {
            //以大写开头的字母为小写，如果包含下划线也会处理为驼峰
            //此处只通过简单的两个标识来判断
            Object value = map.get(key);
            map.remove(key);
            map.put(StrUtil.toCamelCase(key), value);
        }
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
