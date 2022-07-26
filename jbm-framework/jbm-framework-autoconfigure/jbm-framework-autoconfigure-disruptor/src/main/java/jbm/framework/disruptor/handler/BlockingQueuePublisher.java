package jbm.framework.disruptor.handler;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.lmax.disruptor.dsl.Disruptor;
import jbm.framework.disruptor.core.EventProcesssBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class BlockingQueuePublisher<M> extends AbstractExecutionThreadService {
    private static Logger logger = LoggerFactory.getLogger(BlockingQueuePublisher.class);

    private final BlockingQueue<M> blockingQueue;

    private final Disruptor<EventProcesssBean> disruptor;

    public BlockingQueuePublisher(BlockingQueue<M> blockingQueue, Disruptor<EventProcesssBean> disruptor) {
        super();
        this.blockingQueue = blockingQueue;
        this.disruptor = disruptor;
    }

    @Override
    public void run() {
        while (true) {
            try {
                M data = blockingQueue.take();
                DisruptorEventTranslator eventTranslator = new DisruptorEventTranslator(data);
                this.disruptor.publishEvent(eventTranslator);
            } catch (InterruptedException e) {
                logger.error("队列获取数据错误", e);
            }
        }
    }

}
