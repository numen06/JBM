package test.flow;

import cn.hutool.core.thread.ThreadUtil;
import com.ebay.bascomtask.core.Orchestrator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import test.flow.process.ProcessA;
import test.flow.process.ProcessB;
import test.flow.process.ProcessC;
import test.flow.source.JsonDataFlowSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DataFlowTest {

    private Orchestrator $ = Orchestrator.create();

    @Test
    public void test() {
        JsonDataFlowSource source = new JsonDataFlowSource();
        ProcessA processA = new ProcessA();
        ProcessB processB = new ProcessB();
        ProcessC processC = new ProcessC();
        $.activate(source.load()).thenApply(json -> processA.process(json)).thenApply(s -> processB.process(s))
                .thenAccept(s -> processC.process(s));

    }

    @Test
    public void sichabei() {
        //任务1：洗水壶->烧开水
        CompletableFuture<Void> f1 = CompletableFuture
                .runAsync(() -> {
                    log.info("T1:洗水壶...");
                    ThreadUtil.safeSleep(TimeUnit.SECONDS.toMillis(1));

                    log.info("T1:烧开水...");
                    ThreadUtil.safeSleep(TimeUnit.SECONDS.toMillis(15));
                });
        //任务2：洗茶壶->洗茶杯->拿茶叶
        CompletableFuture<String> f2 = CompletableFuture
                .supplyAsync(() -> {
                    log.info("T2:洗茶壶...");
                    ThreadUtil.safeSleep(TimeUnit.SECONDS.toMillis(1));

                    log.info("T2:洗茶杯...");
                    ThreadUtil.safeSleep(TimeUnit.SECONDS.toMillis(2));

                    log.info("T2:拿茶叶...");
                    ThreadUtil.safeSleep(TimeUnit.SECONDS.toMillis(1));
                    return "龙井";
                });
        //任务3：任务1和任务2完成后执行：泡茶
        CompletableFuture<String> f3 = f1.thenCombine(f2, (__, tf) -> {
            log.info("T1:拿到茶叶:" + tf);
            log.info("T1:泡茶...");
            return "上茶:" + tf;
        });
        //等待任务3执行结果
        log.info(f3.join());
    }

}
