package com.jbm.framework.opcua.attribute;

public enum ValueType {

    HEX, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
