package jbm.framework.boot.autoconfigure.taskflow2.test;


import cn.hutool.core.util.StrUtil;
import com.ebay.bascomtask.core.Orchestrator;
import com.ebay.bascomtask.core.TaskInterface;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

interface IEchoTask extends TaskInterface<IEchoTask> {
    CompletableFuture<String> echo(String s);

      CompletableFuture<String> test(String s);
    

}

interface ICombinerTask extends TaskInterface<ICombinerTask> {
    CompletableFuture<String> combine(CompletableFuture<String> a, CompletableFuture<String> b);

    CompletableFuture<String> concat(CompletableFuture<String> cfh, String s) throws ExecutionException, InterruptedException;
}

class EchoTask implements IEchoTask {
    public CompletableFuture<String> echo(String s) {
        return complete(s);
    }

    @Override
    public CompletableFuture<String> test(String s) {
        return complete(s);
    }

}

class CombinerTask implements ICombinerTask {
    public CompletableFuture<String> combine(CompletableFuture<String> a, CompletableFuture<String> b) {
        return complete(get(a) + ' ' + get(b));
    }

    @Override
    public CompletableFuture<String> concat(CompletableFuture<String> cfh, String s) throws ExecutionException, InterruptedException {
        return complete(StrUtil.concat(true,cfh.get(), s));
    }
}

public class TaskTest {

    private Orchestrator $ = Orchestrator.create();
    @Test
    public void getMessage() throws ExecutionException, InterruptedException {
        CompletableFuture<String> left = $.task(new EchoTask()).test("Hello");
        CompletableFuture<String> right = $.task(new EchoTask()).echo("world");
        CompletableFuture<String> combine = $.task(new CombinerTask()).combine(left, right);
        System.out.println(combine.get());
    }
    @Test
    public void appc() throws ExecutionException, InterruptedException {
        CompletableFuture cfh = CompletableFuture.supplyAsync(() -> "hello");
        CombinerTask concatTask = new CombinerTask();
//        CompletableFuture cfw = $.task(concatTask).concat(cfh," world");
        $.task(concatTask).activate().concat(cfh," world").thenAccept(System.out::println);}
}
