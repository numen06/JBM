package jbm.framework.boot.autoconfigure.amqp.usage;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SerializerMessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FastJsonMessageConverter extends AbstractMessageConverter {
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final Map<String, Function<Message, Object>> MSG_CONVERT_MAP = new HashMap<>();

    static {
        MSG_CONVERT_MAP.put(MessageProperties.CONTENT_TYPE_BYTES, message -> message.getBody());
        MSG_CONVERT_MAP.put(MessageProperties.CONTENT_TYPE_TEXT_PLAIN, message -> {
            try {
                return new String(message.getBody(), DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException e) {
                throw new MessageConversionException(
                        "Failed to convert Message content", e);
            }
        });
        MSG_CONVERT_MAP.put(MessageProperties.CONTENT_TYPE_JSON, message -> {
            try {
                ParserConfig parserConfig = new ParserConfig();
                parserConfig.setAutoTypeSupport(true);
                return JSON.parse(new String(message.getBody(), DEFAULT_CHARSET), parserConfig);
            } catch (UnsupportedEncodingException e) {
                throw new MessageConversionException(
                        "Failed to convert Message content", e);
            }
        });
        MSG_CONVERT_MAP.put(MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT, message -> {
            return new SerializerMessageConverter().fromMessage(message);
        });
    }

    private SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();

    public FastJsonMessageConverter() {
        super();
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        try {
            MessageProperties messageProperties = message.getMessageProperties();
            if (MSG_CONVERT_MAP.containsKey(messageProperties.getContentType())) {
                return MSG_CONVERT_MAP.get(messageProperties.getContentType()).apply(message);
            }
        } catch (Exception e) {
            new MessageConversionException(e.getMessage());
        }
        return simpleMessageConverter.fromMessage(message);
    }

    @Override
    protected Message createMessage(Object objectToConvert,
                                    MessageProperties messageProperties)
            throws MessageConversionException {
        byte[] bytes = null;
        try {
            if (objectToConvert instanceof byte[]) {
                messageProperties.setContentType(MessageProperties.CONTENT_TYPE_BYTES);
                bytes = (byte[]) objectToConvert;
            } else if (objectToConvert instanceof CharSequence) {
                messageProperties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
                messageProperties.setContentEncoding(DEFAULT_CHARSET);
                bytes = objectToConvert.toString().getBytes(DEFAULT_CHARSET);
            } else {
                messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                String jsonString = JSON.toJSONString(objectToConvert, SerializerFeature.WriteClassName);
                bytes = jsonString.getBytes(DEFAULT_CHARSET);
            }
        } catch (UnsupportedEncodingException e) {
            throw new MessageConversionException(
                    "Failed to convert Message content", e);
        }
        if (bytes != null) {
            messageProperties.setContentLength(bytes.length);
        }
        return new Message(bytes, messageProperties);
    }

}
