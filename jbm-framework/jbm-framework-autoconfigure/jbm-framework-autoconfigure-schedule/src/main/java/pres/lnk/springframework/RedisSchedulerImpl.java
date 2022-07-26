package pres.lnk.springframework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @Author wesley.zhang
 * @Date 2018/2/28
 */
public class RedisSchedulerImpl extends AbstractScheduler {
    private static final String CACHE_PREFIX = "scheduler_";
    private static final String MAX_LEVEL = "maxLevel";

    @Autowired
    private RedisTemplate redisTemplate;

    private static String prefixKey(String key) {
        return CACHE_PREFIX.concat(key.replaceAll("\\W+", "_"));
    }

    @Override
    public boolean check(String id) {
        Long time = getCache(id);
        return time == null || currentTimeMillis() > time;
    }

    @Override
    public boolean lock(String id, long timeoutMillis) {
        String key = prefixKey(id);
        long nextTimeMillis = currentTimeMillis() + timeoutMillis;
        boolean flag = redisTemplate.opsForValue().setIfAbsent(key, nextTimeMillis);
        if (flag) {
            redisTemplate.expire(key, timeoutMillis < 0 ? 1 : timeoutMillis, TimeUnit.MILLISECONDS);
        } else {
            Long time = getCache(id);
            if (time == null || currentTimeMillis() > time) {
                //避免极端情况下，锁设置失效时间失败，导致了死锁发生，如果锁太久没被释放就主动删除锁
                removeCache(id);
                //删除锁之后再重新抢锁
                return lock(id, timeoutMillis);
            }
        }
        return flag;
    }

    @Override
    public void relock(String id, long timeoutMillis) {
        String key = prefixKey(id);
        redisTemplate.expire(key, timeoutMillis < 0 ? 1 : timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public long currentTimeMillis() {
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        long time = connection.time();
        connection.close();
        return time;
    }

    @Override
    public void keepAlive() {
        int level = getLevel();
        String key = prefixKey(MAX_LEVEL);
        Integer maxLevel = getCache(MAX_LEVEL);
        //如果maxLevel，就将当前level写进去
        if (maxLevel == null) {
            //控制只能写一个，防止并发低级别把高级别覆盖了
            boolean result = redisTemplate.opsForValue().setIfAbsent(key, level);
            if (result) {
                //写入成功，添加过期时间，过期时间追加5秒，避免出现误差，在下次刷新时间前就失效了，被低级别的服务器抢先执行任务
                redisTemplate.expire(key, getHeartTime() + 5, TimeUnit.SECONDS);
                return;
            } else {
                //写入失败，则获取最高级别跟当前级别做比较，避免低级别抢先写入成功
                maxLevel = getCache(key);
            }
        }

        //如果当前级别比最高级别还要高，则覆盖它
        if (maxLevel > level) {
            redisTemplate.delete(key);
            keepAlive();
            return;
        }

        //如果当前级别跟最高级别同级，则刷新过期时间
        if (maxLevel == level) {
            Long expire = redisTemplate.getExpire(prefixKey(MAX_LEVEL));
            //只有过期时间有还有10秒的时候才刷新时间，避免多个服务器同时刷新
            if (expire < 10) {
                //加5秒理由同上
                redisTemplate.expire(key, getHeartTime() + 5, TimeUnit.SECONDS);
            }
        }

    }

    @Override
    public int getMaxAliveLevel() {
        Integer level = getCache(MAX_LEVEL);
        if (level != null) {
            return level;
        }
        //如果没有最高级别则返回当前服务器级别
        return getLevel();
    }

    public <T> T getCache(String key) {
        key = prefixKey(key);
        return (T) redisTemplate.opsForValue().get(key);
    }

    public void removeCache(String key) {
        key = prefixKey(key);
        if (redisTemplate.hasKey(key)) {
            redisTemplate.delete(key);
        }
    }
}
