package jbm.framework.boot.autoconfigure.redis.distributed;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.Data;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 生产序列号工具
 */
public class SerialNumberTamplete {

    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 根据应用名称和分组构建前缀字符串
     *
     * @param appName 应用名称
     * @param groups  分组
     * @return 构建的前缀字符串
     */
    private String buildPrefix(String appName, final String... groups) {

        String prefix = StrUtil.format("{}:{}:{}", StrUtil.emptyToDefault(StrUtil.trim(appName), "JBM"), "SerialNumber", StrUtil.join(":", groups));
        return prefix;
    }

    @Data
    class DaySerialNumber {
        private String dayForamt = "yyyyMMdd";
        private String snLong = "%04d";
        private String snForamt = "{}{}-{}";
    }

    /**
     * 生成指定应用的日期+业务id+序列号的唯一标识
     *
     * @param appName         应用名称
     * @param daySerialNumber 日序号设置
     * @param groups          业务分组，可传入多个参数
     * @return 生成的唯一标识
     */
    public String createDaySerialNumber(String appName, DaySerialNumber daySerialNumber, final String... groups) {
        // 当天日期，比如20221226
        String date = DateUtil.format(DateTime.now(), daySerialNumber.getDayForamt());
        String redisKey = this.buildPrefix(appName, ArrayUtil.append(groups, date));
        // 利用increment即redis原生incrBy命令的原子性特性生成递增的序列号
        Long increment = redisTemplate.opsForValue().increment(redisKey);
        if (increment == null) {
            throw new RuntimeException("创建序列号失败");
        }
        redisTemplate.expire(redisKey, 25, TimeUnit.HOURS);
        String bus = "";
        if (ArrayUtil.isNotEmpty(groups)) {
            bus = ArrayUtil.get(groups, 0);
        }
        // 组合  20221226 + 业务id + 0001(可以根据需要自由调整序列号的长度)
        return StrUtil.format(daySerialNumber.snForamt, bus, date, String.format(daySerialNumber.getSnLong(), increment));
    }


    public String createAppDaySerialNumber(final String... groups) {
        String appName = SpringUtil.getApplicationName();
        return createDaySerialNumber(appName, new DaySerialNumber(), groups);
    }

    /**
     * 初始化序列号
     *
     * @param appName 应用名称
     * @param groups  组别
     * @param number  序列号
     */
    public void initSerialNumber(String appName, final Integer number, final String... groups) {
        if (StrUtil.isEmpty(appName)) {
            throw new RuntimeException("appName is null");
        }

        redisTemplate.opsForValue().set(this.buildPrefix(appName, groups), number);
    }

    public void initAppSerialNumber(final Integer number, final String... groups) {
        String appName = SpringUtil.getApplicationName();
        redisTemplate.opsForValue().set(this.buildPrefix(appName, groups), number);
    }


    /**
     * 创建全局序列号
     *
     * @param groups 组名
     */
    public Integer createGlobalSerialNumber(final String... groups) {
        return this.createSerialNumber(null, groups);
    }


    /**
     * 创建全局序列号
     *
     * @param appName 应用名称
     * @param groups  组别
     * @return 全局序列号
     */
    public Integer createSerialNumber(String appName, final String... groups) {
        if (StrUtil.isEmpty(appName)) {
            throw new RuntimeException("appName is null");
        }
        Integer increment = redisTemplate.opsForValue().increment(this.buildPrefix(appName, groups)).intValue();
        if (increment == null) {
            throw new RuntimeException("创建序列号失败");
        }
        return increment;
    }

    /**
     * 创建一个应用程序序列号
     *
     * @param groups 序列号所属的组
     * @return 序列号
     */
    public Integer createAppSerialNumber(final String groups) {
        String appName = SpringUtil.getApplicationName();
        return this.createSerialNumber(appName, groups);
    }
}
