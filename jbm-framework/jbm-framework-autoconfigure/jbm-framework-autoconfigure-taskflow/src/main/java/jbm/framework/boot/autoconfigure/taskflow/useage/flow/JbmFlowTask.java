package jbm.framework.boot.autoconfigure.taskflow.useage.flow;

import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jbm.framework.boot.autoconfigure.taskflow.annotation.JbmWork;
import org.springframework.cglib.core.ReflectUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-11 16:22
 **/
public class JbmFlowTask implements IFlowTask {

    /**
     * 执行任务流
     */
    private final Map<Integer, IFlowTask> tasks = Maps.newTreeMap();

    /**
     * 执行级别
     */
    private AtomicInteger level = new AtomicInteger(0);

    @Override
    public final IFlowTask after(IFlowTask jbmFlow) {
        tasks.put(level.getAndAdd(1), jbmFlow);
        return this;
    }

    @Override
    public final IFlowTask add(IFlowTask jbmFlow) {
        tasks.put(level.get(), jbmFlow);
        return this;
    }

    @Override
    public void action() {
        for (Integer lev : tasks.keySet()) {

        }
    }

    @JbmWork
    public void test(String xx) {
        System.out.println(xx);
    }

    public static void main(String[] args) {
        JbmFlowTask task = new JbmFlowTask();
        List<Method> methodLits = Lists.newArrayList();
        ReflectUtils.addAllMethods(JbmFlowTask.class, methodLits);
        Method[] testMethod = ReflectUtil.getMethods(JbmFlowTask.class, new Filter<Method>() {
            @Override
            public boolean accept(Method method) {
                JbmWork jbmWork = method.getAnnotation(JbmWork.class);
                return ObjectUtil.isNotEmpty(jbmWork);
            }
        });
        System.out.println(JSON.toJSONString(testMethod));
//        Methref.on(JbmFlowTask.class).to().test("test");
//        task.test("test");
    }

}
