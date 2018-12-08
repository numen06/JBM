package org.springframework.data.level.serializer;

import java.nio.charset.Charset;

import org.springframework.util.Assert;

public class StringLevelSerializer implements LevelSerializer<String> {

    private final Charset charset;

    public StringLevelSerializer() {
        this(Charset.forName("UTF8"));
    }

    public StringLevelSerializer(Charset charset) {
        Assert.notNull(charset, "字符为空");
        this.charset = charset;
    }

    public String deserialize(byte[] bytes) {
        return (bytes == null ? null : new String(bytes, charset));
    }

    public byte[] serialize(String string) {
        return (string == null ? null : string.getBytes(charset));
    }
}
