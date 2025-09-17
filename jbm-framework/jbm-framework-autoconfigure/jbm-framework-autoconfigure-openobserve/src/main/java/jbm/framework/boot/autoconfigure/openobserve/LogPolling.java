package jbm.framework.boot.autoconfigure.openobserve;

import com.google.common.util.concurrent.AbstractScheduledService;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryBean;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryResult;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
public class LogPolling extends AbstractScheduledService {


    private final OpenObserveTemplate openObserveTemplate;

    private LinkedBlockingQueue<QueryResult> queryBeanQueue = new LinkedBlockingQueue<>(10);

    public LogPolling(OpenObserveTemplate openObserveTemplate) {
        this.openObserveTemplate = openObserveTemplate;
        this.startAsync();
    }

    private final AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());

    private QueryBean queryBean;

    private final Executor executor = Executors.newSingleThreadScheduledExecutor();

    public void look(QueryBean queryBean, Consumer<QueryResult> queryResultConsumer) {
        this.queryBean = queryBean;
        initQueryBean();
        //异步回调
        executor.execute(() -> {
            while (true) {
                QueryResult queryResult = null;
                try {
                    queryResult = queryBeanQueue.take();
                } catch (InterruptedException e) {
                    continue;
                }
                queryResultConsumer.accept(queryResult);
            }
        });
    }

    private void initQueryBean() {
        if (queryBean == null) {
            throw new NullPointerException();
        }
        if (queryBean.getQuery() == null) {
            throw new NullPointerException();
        }
        if (queryBean.getQuery().getEndTime() == null) {
            queryBean.getQuery().setEndTime(lastTime.get());
        }
        if (queryBean.getQuery().getStartTime() == null) {
            queryBean.getQuery().setStartTime(lastTime.getAndAdd(-1000 * 60));
        }

    }

    private void nextQueryBean() {
        lastTime.set(queryBean.getQuery().getEndTime());
        queryBean.getQuery().setStartTime(lastTime.get());
        queryBean.getQuery().setEndTime(System.currentTimeMillis() * 1000);
    }


    /**
     * @throws Exception
     */
    @Override
    protected void runOneIteration() throws Exception {
        if (queryBean == null) {
            return;
        }
        try {
            QueryResult queryResult = this.openObserveTemplate.selectLogs(queryBean);
            if (queryResult == null) {
                return;
            }
            if (queryResult.getHits() == null) {
                return;
            }
            if (queryResult.getHits().isEmpty()) {
                return;
            }
//            if (queryResult.getTotal() <= 0) {
//                return;
//            }
            queryBeanQueue.put(queryResult);
        } catch (Exception e) {
            log.error("查询日志失败", e);
        } finally {
            nextQueryBean();
        }

    }

    /**
     * @return
     */
    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1, 1, TimeUnit.SECONDS);
    }
}
