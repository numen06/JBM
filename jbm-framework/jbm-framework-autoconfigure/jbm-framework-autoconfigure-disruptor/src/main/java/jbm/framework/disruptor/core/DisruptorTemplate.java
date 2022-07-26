package jbm.framework.disruptor.core;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import java.io.Serializable;
import java.util.concurrent.ThreadFactory;

public class DisruptorTemplate<T extends Serializable> {

    private final Disruptor<T> disruptor;

    public DisruptorTemplate(EventFactory<T> eventFactory) {
        this(eventFactory, new ThreadPoolExecutorFactoryBean(), 256);
    }

    public DisruptorTemplate(EventFactory<T> eventFactory, ThreadFactory threadFactory, Integer bufferSize) {
        super();
        ThreadPoolExecutorFactoryBean pool = new ThreadPoolExecutorFactoryBean();
        pool.setCorePoolSize(10);
        this.disruptor = new Disruptor<T>(eventFactory, bufferSize, pool);
    }

    public DisruptorTemplate(Disruptor<T> disruptor) {
        super();
        this.disruptor = disruptor;
    }

    public Disruptor<T> getDisruptor() {
        return disruptor;
    }

}
