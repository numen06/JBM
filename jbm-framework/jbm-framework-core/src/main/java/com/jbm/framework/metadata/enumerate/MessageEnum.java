package com.jbm.framework.metadata.enumerate;

public enum MessageEnum {
    error(0), success(1), warn(2), notice(3);

    private int value;

    private MessageEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

}
