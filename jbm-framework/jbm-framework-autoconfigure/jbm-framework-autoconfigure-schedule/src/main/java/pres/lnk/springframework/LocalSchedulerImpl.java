package pres.lnk.springframework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地调试器，只能在单服务器下使用，不能用在集群服务中
 *
 * @Author lnk
 * @Date 2018/2/28
 */
public class LocalSchedulerImpl extends AbstractScheduler {
    private static Map<String, Long> cache = new ConcurrentHashMap<>();

    @Override
    public boolean check(String id) {
        //获取任务下次执行时间，并判断当前时间是否已经到了执行时间
        Long time = cache.get(id);
        return time == null || currentTimeMillis() > time;
    }

    @Override
    public boolean lock(String id, long timeoutMillis) {
        //设置下次执行时间，如果设置成功则返回获取锁成功
        Integer lockObj = getLockObj(id);
        synchronized (lockObj) {
            if (check(id)) {
                cache.put(id, currentTimeMillis() + timeoutMillis);
                return true;
            }
        }
        return false;
    }

    @Override
    public void relock(String id, long timeoutMillis) {
        cache.put(id, currentTimeMillis() + timeoutMillis);
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 利用jdk内部int[-128 ~ 127]范围返回是同一个对象的特性，生成字符串的锁
     * 同一个字符串的锁肯定是同一个，保证锁的同步
     * 最多同时产生0 ~ 127之间的128个同步锁
     *
     * @param str
     * @return
     */
    private static int getLockObj(String str) {
        return str.hashCode() % 127;
    }

}
