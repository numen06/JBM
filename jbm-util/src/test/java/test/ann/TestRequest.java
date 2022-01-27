package test.ann;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestRequest {

    String value() default "";

    String formTopic();

    String toTopic();

}
