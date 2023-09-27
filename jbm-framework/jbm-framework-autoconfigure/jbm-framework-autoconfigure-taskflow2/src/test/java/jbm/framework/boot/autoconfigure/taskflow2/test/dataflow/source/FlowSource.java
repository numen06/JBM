package jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.source;

import com.ebay.bascomtask.core.TaskInterface;

import java.util.concurrent.CompletableFuture;

public interface FlowSource extends TaskInterface<FlowSource> {

 public    CompletableFuture<String> load();
}
