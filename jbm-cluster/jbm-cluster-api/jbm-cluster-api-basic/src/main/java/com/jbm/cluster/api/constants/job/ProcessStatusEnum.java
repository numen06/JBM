package com.jbm.cluster.api.constants.job;

import com.jbm.framework.dictionary.annotation.JbmDicCode;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import com.jbm.framework.dictionary.annotation.JbmDicValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程状态枚举
 *
 * @author scolin
 * @date 2025/11/17
 */
@Getter
@JbmDicType(typeName = "流程状态", value = "process_status")
public enum ProcessStatusEnum {

    /**
     * 等待中
     */
    WAITING("等待中"),

    /**
     * 运行中
     */
    RUNNING("运行中"),

    /**
     * 已完成
     */
    COMPLETED("已完成"),

    /**
     * 已触发
     */
    TRIGGERED("已触发"),

    /**
     * 已失败
     */
    FAILED("已失败");

    /**
     * 状态值
     */
    @JbmDicCode
    private  String code;

    /**
     * 状态描述
     */
    @JbmDicValue
    private String name;

    ProcessStatusEnum(String name) {
        this.code = this.toString();
        this.name = name;
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
