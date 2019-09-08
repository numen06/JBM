package jbm.framework.boot.autoconfigure.taskflow.spring;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.ebay.bascomtask.main.Orchestrator;
import jbm.framework.boot.autoconfigure.taskflow.useage.JbmTaskClosureGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
public abstract class JbmTaskFlow {

    private Orchestrator orchestrator;


    @Autowired(required = false)
    private JbmTaskClosureGenerator jbmTaskClosureGenerator = new JbmTaskClosureGenerator();


    @PostConstruct
    public final void init() {
        orchestrator = Orchestrator.create().closureGenerator(jbmTaskClosureGenerator);
        this.initSources();
        this.initWorks();
    }

    /**
     * 初始化工作流
     */
    public void initSources() {
        List<Object> objects = this.sources();
        if (CollectionUtil.isEmpty(objects))
            return;
        for (Object object : objects) {
            if (ObjectUtil.isNotEmpty(object))
                orchestrator.addIgnoreTaskMethods(object);
        }
    }


    /**
     * 初始化工作流
     */
    public void initWorks() {
        Class clazz = this.getClass();
        Class innerClazz[] = clazz.getDeclaredClasses();
        for (Class cls : innerClazz) {
            try {
                Object obj = ReflectUtil.newInstance(cls, this);
                orchestrator.addWork(obj);
            } catch (Exception e) {
                log.error("导入任务错误", e);
            }
        }
    }

    public abstract List<Object> sources();


    public final void execute() {
        this.orchestrator.execute();
    }

}
