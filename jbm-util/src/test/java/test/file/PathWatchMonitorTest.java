package test.file;

import cn.hutool.core.thread.ThreadUtil;
import com.jbm.util.file.PathWatchMonitor;
import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

public class PathWatchMonitorTest extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testWatch() {
        PathWatchMonitor watch = new PathWatchMonitor();
        watch.watch();
        ThreadUtil.sleep(5, TimeUnit.MINUTES);
    }
}