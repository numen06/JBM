package jbm.framework.boot.autoconfigure.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ClassUtil;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @Created wesley.zhang
 * @Date 2022/5/21 19:50
 * @Description TODO
 */
public abstract class SimpleUnknownRuntimeExceptionFilter implements UnknownRuntimeExceptionFilter {


    private Map<String, Class<? extends RuntimeException>> runtimeExceptionMap = null;

    public abstract void filterRuntimeExceptions(Set<Class<? extends RuntimeException>> runtimeExceptions);

//    public abstract void filterPackages(Set<String> runtimeExceptionPackages);

    @Override
    public void apply(ResultBody resultBody, RuntimeException runtimeException, HttpServletRequest request) {
        if (runtimeExceptionMap == null) {
            Set<Class<? extends RuntimeException>> runtimeExceptions = new HashSet<>();
            filterRuntimeExceptions(runtimeExceptions);
            if (CollUtil.isNotEmpty(runtimeExceptions)) {
                if (CollUtil.isNotEmpty(runtimeExceptions)) {
                    runtimeExceptionMap = runtimeExceptions.stream().collect(
                            Collectors.toMap(item -> item.getName(),
                                    item -> item,
                                    (oldVal, currVal) -> oldVal,
                                    ConcurrentHashMap::new));
                }
            } else {
                runtimeExceptionMap = new ConcurrentHashMap<>();
            }
        }
        runtimeExceptionMap.forEach(new BiConsumer<String, Class<? extends RuntimeException>>() {
            @Override
            public void accept(String className, Class<? extends RuntimeException> errorException) {
                if (!errorException.isInstance(runtimeException)) {
                    return;
                }
                //默认将错误引入消息
                resultBody.msg(runtimeException.getMessage());
                //擦除报错信息
                resultBody.exception(null);
                //
                return;
            }
        });
//        if (runtimeExceptionMap.containsKey(runtimeException.getClass().getName())) {
//            //默认将错误引入消息
//            resultBody.msg(runtimeException.getMessage());
//            //擦除报错信息
//            resultBody.exception(null);
//            //
//            return;
//        }
    }


}
