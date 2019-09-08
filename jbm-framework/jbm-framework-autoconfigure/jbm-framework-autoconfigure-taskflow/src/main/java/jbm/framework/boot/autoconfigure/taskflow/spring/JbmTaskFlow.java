package jbm.framework.boot.autoconfigure.taskflow.spring;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.ebay.bascomtask.main.Orchestrator;
import com.google.common.collect.Lists;
import jbm.framework.boot.autoconfigure.taskflow.useage.JbmTaskClosureGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
public abstract class JbmTaskFlow {

    private Orchestrator orchestrator;

    private List<Object> sources = Lists.newArrayList();

    private boolean init = false;

    @Autowired(required = false)
    private JbmTaskClosureGenerator jbmTaskClosureGenerator = new JbmTaskClosureGenerator();


    @PostConstruct
    public final void init() {
        sources.clear();
        orchestrator = Orchestrator.create().closureGenerator(jbmTaskClosureGenerator);
//        this.initSources();
        this.initWorks();
        init = true;
    }

    /**
     * 初始化工作流
     */
    private void initSources() {
        if (CollectionUtil.isEmpty(this.sources))
            return;
        for (Object object : this.sources) {
            if (ObjectUtil.isNotEmpty(object))
                orchestrator.addIgnoreTaskMethods(object);
        }
    }


    /**
     * 初始化工作流
     */
    private void initWorks() {
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

    public final void execute(Object... sources) {
        this.execute(Lists.newArrayList(sources));
    }


    public final void execute2(List<Object> sources) {
        if (!init) {
            this.init();
        }
        if (orchestrator.getStats().getNumberOfTasksExecuted() > 0) {
            this.init();
        }
        this.sources.addAll(sources);
        this.initSources();
        this.orchestrator.execute();
    }

    public final void execute() {
        this.execute(Lists.newArrayList());
    }
}
