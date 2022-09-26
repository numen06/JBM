package com.jbm.cluster.job.task;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.job.SysJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 定时任务调度测试
 *
 * @author wesley
 */
@Slf4j
@Component
public class JbmTask {
    public void multipleParams(String s, Boolean b, Long l, Double d, Integer i) {
        log.info(StrUtil.format("执行多参方法： 字符串类型{}，布尔类型{}，长整型{}，浮点型{}，整形{}", s, b, l, d, i));
    }

    public void params(String params) {
        log.info("执行有参方法：" + params);
    }

    public void noParams() {
        log.info("执行无参方法");
    }
}
