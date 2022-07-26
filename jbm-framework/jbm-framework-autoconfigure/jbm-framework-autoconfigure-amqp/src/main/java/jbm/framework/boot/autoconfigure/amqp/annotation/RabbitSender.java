package jbm.framework.boot.autoconfigure.amqp.annotation;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.messaging.handler.annotation.MessageMapping;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@MessageMapping
@Documented
public @interface RabbitSender {

    /**
     * The queues for this listener.
     * The entries can be 'queue name', 'property-placeholder keys' or 'expressions'.
     * Expression must be resolved to the queue name or {@code Queue} object.
     * The queue(s) must exist, or be otherwise defined elsewhere as a bean(s) with
     * a {@link org.springframework.amqp.rabbit.core.RabbitAdmin} in the application
     * context.
     * Mutually exclusive with {@link #bindings()} and {@link #queuesToDeclare()}.
     *
     * @return the queue names or expressions (SpEL) to listen to from target
     * @see org.springframework.amqp.rabbit.listener.MessageListenerContainer
     */
    String[] queues() default {};

    /**
     * The queues for this listener.
     * If there is a {@link org.springframework.amqp.rabbit.core.RabbitAdmin} in the
     * application context, the queue will be declared on the broker with default
     * binding (default exchange with the queue name as the routing key).
     * Mutually exclusive with {@link #bindings()} and {@link #queues()}.
     *
     * @return the queue(s) to declare.
     * @see org.springframework.amqp.rabbit.listener.MessageListenerContainer
     * @since 2.0
     */
    Queue[] queuesToDeclare() default {};

    /**
     * Array of {@link QueueBinding}s providing the listener's queue names, together
     * with the exchange and optional binding information.
     * Mutually exclusive with {@link #queues()} and {@link #queuesToDeclare()}.
     *
     * @return the bindings.
     * @see org.springframework.amqp.rabbit.listener.MessageListenerContainer
     * @since 1.5
     */
    QueueBinding[] bindings() default {};
}
