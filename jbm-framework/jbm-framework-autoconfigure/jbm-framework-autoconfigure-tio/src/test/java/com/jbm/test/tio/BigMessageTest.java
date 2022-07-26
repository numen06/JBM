package com.jbm.test.tio;

public class BigMessageTest {

    private String name;

    private byte[] data;

    public BigMessageTest(String name, byte[] data) {
        super();
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

}
