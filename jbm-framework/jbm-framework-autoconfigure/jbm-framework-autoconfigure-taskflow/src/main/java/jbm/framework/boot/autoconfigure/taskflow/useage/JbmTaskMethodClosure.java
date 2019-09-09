package jbm.framework.boot.autoconfigure.taskflow.useage;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.ebay.bascomtask.main.TaskMethodClosure;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JbmTaskMethodClosure extends TaskMethodClosure {

    @Override
    public void prepare() {
        super.prepare();
    }

    @Override
    public void prepareTaskMethod() {
        super.prepareTaskMethod();
    }


    @Override
    public boolean executeTaskMethod() {
        Object pojo = getTargetPojoTask();
        boolean hit = false;
        boolean worked = false;
        try {
            log.debug("任务{}开始", this.getTaskName());
            if (validator(pojo)) {
                hit = super.executeTaskMethod();
                worked = true;
            } else {
                log.debug("任务被跳过");
            }
            workEnd(pojo, worked);
            log.debug("任务{}结束", this.getTaskName());
        } catch (Exception e) {
            log.error("执行流程错误", e);
            throw e;
        }
        return hit;
    }

    public void workEnd(Object pojo, boolean worked) {
        if (pojo instanceof JbmBaseProcessor) {
            JbmBaseProcessor processor = (JbmBaseProcessor) pojo;
            //没有执行相当于已经终止这条线
            processor.setStop(!worked);
        }
    }

    public boolean validator(Object pojo) {
        if (ArrayUtil.isEmpty(super.getMethodBindings())) {
            return true;
        }
        for (Object arg : super.getMethodBindings()) {
            if (arg instanceof JbmBaseProcessor) {
                JbmBaseProcessor processor = (JbmBaseProcessor) arg;
                ReflectUtil.setFieldValue(pojo, "stop", processor.isStop());
                if (processor.isStop()) {
                    return false;
                }
                if (ObjectUtil.isNotEmpty(processor.getNext())) {
                    if (!pojo.getClass().equals(processor.getNext())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
