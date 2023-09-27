package jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.process;

import com.ebay.bascomtask.core.TaskInterface;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface FlowProcess<S,R> extends TaskInterface<FlowProcess> {



    CompletableFuture<R> process(S source);
}
