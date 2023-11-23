package test.watch;

import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.nio.file.WatchEvent;

public class WatchTest {

    @Test
    public void testWatchTest() {
        // Given
        DelayWatcher watcher = new DelayWatcher(new SimpleWatcher(), 500);
        watcher.onModify(new WatchEvent() {
            @Override
            public Kind kind() {
                return null;
            }

            @Override
            public int count() {
                return 0;
            }

            @Override
            public Object context() {
                return null;
            }
        }, Paths.get("test"));



    }
}
