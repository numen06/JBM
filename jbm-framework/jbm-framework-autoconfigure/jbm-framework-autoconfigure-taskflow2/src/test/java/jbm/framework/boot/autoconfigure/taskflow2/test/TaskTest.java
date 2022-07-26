package jbm.framework.boot.autoconfigure.taskflow2.test;


import com.ebay.bascomtask.core.Orchestrator;
import com.ebay.bascomtask.core.TaskInterface;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

interface IEchoTask extends TaskInterface<IEchoTask> {
    CompletableFuture<String> echo(String s);
}

interface ICombinerTask extends TaskInterface<ICombinerTask> {
    CompletableFuture<String> combine(CompletableFuture<String> a, CompletableFuture<String> b);
}

class EchoTask implements IEchoTask {
    public CompletableFuture<String> echo(String s) {
        return complete(s);
    }
}

class CombinerTask implements ICombinerTask {
    public CompletableFuture<String> combine(CompletableFuture<String> a, CompletableFuture<String> b) {
        return complete(get(a) + ' ' + get(b));
    }
}

public class TaskTest {
    @Test
    public void getMessage() throws ExecutionException, InterruptedException {
        Orchestrator $ = Orchestrator.create();
        CompletableFuture<String> left = $.task(new EchoTask()).echo("Hello");
        CompletableFuture<String> right = $.task(new EchoTask()).echo("world");
        CompletableFuture<String> combine = $.task(new CombinerTask()).combine(left, right);
        System.out.println(combine.get());
    }
}
