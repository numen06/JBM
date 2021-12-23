package com.jbm.cluster.push.msg;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface MqMessageSource {

    // BOSS生产者
    String MESSAGE_OUTPUT = "message_output";
    // EMC消费者
    String Consumer = "emc-message";

    @Output(MESSAGE_OUTPUT)
    MessageChannel messageOutput();

    @Input(Consumer)
    MessageChannel messageInput();

}