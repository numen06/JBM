package test;

import com.jbm.util.thread.InMemoryBlacklistRateLimiter;
import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

public class RateLimiterTest extends TestCase {


    public void testRateLimiter() throws InterruptedException {
        InMemoryBlacklistRateLimiter limiter = new InMemoryBlacklistRateLimiter(1, TimeUnit.MINUTES,50);

        String ip = "192.168.1.1";

        for (int i = 0; i < 65; i++) {
            boolean blocked = limiter.isBlacklisted(ip);
            if (blocked) {
                System.out.println("请求 " + (i + 1) + "：用户 " + ip + " 已被拉入黑名单！");
            } else {
                System.out.println("请求 " + (i + 1) + "：允许访问");
            }
            // 模拟快速请求（每10毫秒一次）
            Thread.sleep(10);
        }

        // 查看当前计数（此时已过部分时间）
        System.out.println("当前访问次数: " + limiter.getRequestCount(ip));
    }
}
