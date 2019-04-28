package pres.lnk.springframework;

import org.apache.logging.log4j.core.util.CronExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

/**
 * @Author wesley.zhang
 * @Date 2018/2/28
 */
public class ScheduledUtil {
    public static final String SCHEDULED_CRON = "cron";
    public static final String SCHEDULED_FIXED_DELAY = "fixedDelay";
    public static final String SCHEDULED_FIXED_RATE = "fixedRate";

    /**
     * 获取定时任务下次的执行时间（精确到毫秒）
     *
     * @param scheduled
     * @param currentTimeMillis
     * @param embeddedValueResolver
     * @return
     */
    public static long getNextTimeMillis(Scheduled scheduled, long currentTimeMillis, StringValueResolver embeddedValueResolver) {
        if (StringUtils.hasText(scheduled.cron())) {
            try {
                CronExpression exp = new CronExpression(scheduled.cron());
                String zone = scheduled.zone();
                TimeZone timeZone;
                if (StringUtils.hasText(zone)) {
                    timeZone = StringUtils.parseTimeZoneString(zone);
                    exp.setTimeZone(timeZone);
                }
                Date nextTime = exp.getNextValidTimeAfter(new Date(currentTimeMillis));
                return nextTime.getTime();
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if (scheduled.fixedDelay() > 0 || StringUtils.hasText(scheduled.fixedDelayString())) {
            if (scheduled.fixedDelay() > 0) {
                return currentTimeMillis + scheduled.fixedDelay();
            }

            String fixedDelayString = scheduled.fixedDelayString();
            fixedDelayString = embeddedValueResolver.resolveStringValue(fixedDelayString);
            long fixedDelay = Long.parseLong(fixedDelayString);

            return currentTimeMillis + fixedDelay;
        }
        if (scheduled.fixedRate() > 0 || StringUtils.hasText(scheduled.fixedRateString())) {
            if (scheduled.fixedRate() > 0) {
                return currentTimeMillis + scheduled.fixedRate();
            }

            String fixedRateString = scheduled.fixedRateString();
            fixedRateString = embeddedValueResolver.resolveStringValue(fixedRateString);
            long fixedRate = Long.parseLong(fixedRateString);

            return currentTimeMillis + fixedRate;
        }
        return Long.MAX_VALUE;
    }

    /**
     * 获取下次执行任务的间隔时间(精确到毫秒)
     *
     * @param scheduled
     * @param embeddedValueResolver
     * @return
     */
    public static long getNextTimeInterval(Scheduled scheduled, StringValueResolver embeddedValueResolver) {
        String cron = scheduled.cron();
        if (StringUtils.hasText(cron)) {
            try {
                String zone = scheduled.zone();
                if (embeddedValueResolver != null) {
                    cron = embeddedValueResolver.resolveStringValue(cron);
                    zone = embeddedValueResolver.resolveStringValue(zone);
                }
                CronExpression exp = new CronExpression(cron);

                TimeZone timeZone;
                if (StringUtils.hasText(zone)) {
                    timeZone = StringUtils.parseTimeZoneString(zone);
                    exp.setTimeZone(timeZone);
                }

                long currentTimeMillis = System.currentTimeMillis();
                Date nextTime = exp.getNextValidTimeAfter(new Date(currentTimeMillis));
                return nextTime.getTime() - currentTimeMillis;
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (scheduled.fixedDelay() > 0 || StringUtils.hasText(scheduled.fixedDelayString())) {
            if (scheduled.fixedDelay() > 0) {
                return scheduled.fixedDelay();
            }

            String fixedDelayString = scheduled.fixedDelayString();
            fixedDelayString = embeddedValueResolver.resolveStringValue(fixedDelayString);
            long fixedDelay = Long.parseLong(fixedDelayString);

            return fixedDelay;
        }
        if (scheduled.fixedRate() > 0 || StringUtils.hasText(scheduled.fixedRateString())) {
            if (scheduled.fixedRate() > 0) {
                return scheduled.fixedRate();
            }

            String fixedRateString = scheduled.fixedRateString();
            fixedRateString = embeddedValueResolver.resolveStringValue(fixedRateString);
            long fixedRate = Long.parseLong(fixedRateString);

            return fixedRate;
        }

        return 0;
    }

    /**
     * 获取定时任务执行时间的计算方式类型
     *
     * @param scheduled
     * @return
     */
    public static String getType(Scheduled scheduled) {
        if (StringUtils.hasText(scheduled.cron())) {
            return SCHEDULED_CRON;
        } else if (scheduled.fixedDelay() > 0 || StringUtils.hasText(scheduled.fixedDelayString())) {
            return SCHEDULED_FIXED_DELAY;
        } else if (scheduled.fixedRate() > 0 || StringUtils.hasText(scheduled.fixedRateString())) {
            return SCHEDULED_FIXED_RATE;
        }
        return null;
    }

    /**
     * 获取定时任务执行时间的计算方式类型
     *
     * @param scheduled
     * @return
     */
    public static String getFlag(Scheduled scheduled, StringValueResolver embeddedValueResolver) {
        if (StringUtils.hasText(scheduled.cron())) {
            return SCHEDULED_CRON + "_" + scheduled.cron().replaceAll(" ", "");
        } else if (scheduled.fixedDelay() > 0 || StringUtils.hasText(scheduled.fixedDelayString())) {
            long fixedDelay = scheduled.fixedDelay();
            if (fixedDelay <= 0) {
                String fixedDelayString = scheduled.fixedDelayString();
                fixedDelayString = embeddedValueResolver.resolveStringValue(fixedDelayString);
                fixedDelay = Long.parseLong(fixedDelayString);
            }

            return SCHEDULED_FIXED_DELAY + "_" + fixedDelay;
        } else if (scheduled.fixedRate() > 0 || StringUtils.hasText(scheduled.fixedRateString())) {
            long fixedRate = scheduled.fixedRate();
            if (fixedRate <= 0) {
                String fixedRateString = scheduled.fixedRateString();
                fixedRateString = embeddedValueResolver.resolveStringValue(fixedRateString);
                fixedRate = Long.parseLong(fixedRateString);
            }

            return SCHEDULED_FIXED_RATE + "_" + fixedRate;
        }
        return null;
    }
}
