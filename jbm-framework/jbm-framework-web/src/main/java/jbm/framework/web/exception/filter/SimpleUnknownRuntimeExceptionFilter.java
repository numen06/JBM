package jbm.framework.web.exception.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.exceptions.base.BaseException;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.spring.MessageUtils;
import jbm.framework.web.exception.UnknownRuntimeExceptionFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;

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
@Slf4j
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
                            Collectors.toMap(item -> ClassUtil.getClassName(item, false),
                                    item -> item,
                                    (oldVal, currVal) -> oldVal,
                                    ConcurrentHashMap::new));
                }
            } else {
                runtimeExceptionMap = new ConcurrentHashMap<>();
            }
        }
        if (runtimeExceptionMap.containsKey(runtimeException.getClass().getName())) {
            //默认将错误引入消息
            resultBody.msg(runtimeException.getMessage());
            //擦除报错信息
            resultBody.exception(null);
            //
            return;
        }
        runtimeExceptionMap.forEach(new BiConsumer<String, Class<? extends RuntimeException>>() {
            @Override
            public void accept(String className, Class<? extends RuntimeException> errorException) {
                if (!errorException.isInstance(runtimeException)) {
                    return;
                }
                String error = runtimeException.getMessage();
                if (runtimeException instanceof BaseException) {
                    error = getBaseExceptionMessage((BaseException) runtimeException);
                }
                //默认将错误引入消息
                resultBody.msg(error);
                //擦除报错信息
                resultBody.exception(null);
                //
                return;
            }
        });

    }


    public String getBaseExceptionMessage(BaseException baseException) {
        String message = null;
        try {
            if (!StrUtil.isEmpty(baseException.getCode())) {
                message = MessageUtils.message(baseException.getCode(), baseException.getArgs());
            }
        } catch (NoSuchMessageException noSuchMessageException) {
            log.warn("没有配置错误国际化:{}", baseException.getCode());
        }
        if (message == null) {
            message = baseException.getDefaultMessage();
        }
        return message;
    }


}
