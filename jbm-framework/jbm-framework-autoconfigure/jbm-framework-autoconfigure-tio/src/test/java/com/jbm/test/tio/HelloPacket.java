package com.jbm.test.tio;

import org.tio.core.intf.Packet;

public class HelloPacket extends Packet {
    public static final int HEADER_LENGHT = 4;// 消息头的长度
    public static final String CHARSET = "utf-8";
    private static final long serialVersionUID = -172060606924066412L;
    private byte[] body;

    /**
     * @return the body
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * @param body the body to set
     */
    public void setBody(byte[] body) {
        this.body = body;
    }
}