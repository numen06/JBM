package com.jbm.cluster.api.constants.job;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程状态枚举
 *
 * @author scolin
 * @date 2025/11/17
 */
@Getter
@AllArgsConstructor
public enum ProcessStatusEnum {

    /**
     * 等待中
     */
    WAITING("WAITING", "等待中"),

    /**
     * 运行中
     */
    RUNNING("RUNNING", "运行中"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 已触发
     */
    TRIGGERED("TRIGGERED", "已触发"),

    /**
     * 已失败
     */
    FAILED("FAILED", "已失败");

    /**
     * 状态值
     */
    private final String value;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据状态值获取枚举
     *
     * @param value 状态值
     * @return 枚举实例，不存在时返回null
     */
    public static ProcessStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ProcessStatusEnum status : ProcessStatusEnum.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为成功状态
     *
     * @return true表示成功，false表示失败或其他
     */
    public boolean isSuccess() {
        return this == COMPLETED;
    }

    /**
     * 判断是否为失败状态
     *
     * @return true表示失败，false表示成功或其他
     */
    public boolean isFailed() {
        return this == FAILED;
    }

    /**
     * 判断是否为等待状态
     *
     * @return true表示等待，false表示其他
     */
    public boolean isWaiting() {
        return this == WAITING;
    }

    /**
     * 判断是否为运行中状态
     *
     * @return true表示运行中，false表示其他
     */
    public boolean isRunning() {
        return this == RUNNING;
    }
}
